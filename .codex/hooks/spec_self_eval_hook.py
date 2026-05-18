#!/usr/bin/env python3
"""Codex hook that runs spec-self-eval when .specs/<feature>/ changes."""

from __future__ import annotations

import datetime as dt
import hashlib
import json
import os
import re
import shlex
import subprocess
import sys
from pathlib import Path
from typing import Any


def main() -> int:
    payload = read_payload()
    repo = repo_root()
    state_dir = hook_state_dir(repo)
    event = str(payload.get("hook_event_name") or payload.get("event") or "")

    if event == "UserPromptSubmit":
        state_dir.mkdir(parents=True, exist_ok=True)
        snapshot = {
            "createdAt": now_iso(),
            "key": state_key(payload),
            "files": snapshot_specs(repo),
        }
        write_json(state_dir / f"{state_key(payload)}.json", snapshot)
        write_json(state_dir / "latest.json", snapshot)
        return 0

    if event and event != "Stop":
        return 0

    touched_features = touched_spec_features(repo, state_dir, payload)
    if not touched_features:
        return 0

    failures: list[str] = []
    evaluated: list[str] = []
    for feature in touched_features:
        report = run_spec_self_eval(repo, feature)
        evaluated.append(display_path(repo, report))
        failures.extend(parse_failures(report, feature))

    if failures:
        reason = [
            "spec-self-eval found [FAIL] items after .specs changes.",
            "Fix the spec before ending the turn.",
            "",
            "Reports:",
            *[f"- {path}" for path in evaluated],
            "",
            "Failed items:",
            *[f"- {failure}" for failure in failures],
        ]
        print(json.dumps({"decision": "block", "reason": "\n".join(reason)}))
    return 0


def read_payload() -> dict[str, Any]:
    raw = sys.stdin.read().strip()
    if not raw:
        return {}
    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        return {}
    return data if isinstance(data, dict) else {}


def repo_root() -> Path:
    proc = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
        check=False,
    )
    if proc.returncode == 0 and proc.stdout.strip():
        return Path(proc.stdout.strip()).resolve()
    return Path.cwd().resolve()


def display_path(repo: Path, path: Path) -> str:
    try:
        return str(path.relative_to(repo))
    except ValueError:
        return str(path)


def hook_state_dir(repo: Path) -> Path:
    override = os.environ.get("SPEC_SELF_EVAL_STATE_DIR")
    if override:
        return Path(override).expanduser()
    repo_key = hashlib.sha256(str(repo).encode("utf-8")).hexdigest()[:16]
    return Path("/tmp") / "codex-spec-self-eval-hooks" / repo_key


def state_key(payload: dict[str, Any]) -> str:
    raw = (
        payload.get("turn_id")
        or payload.get("session_id")
        or payload.get("transcript_path")
        or "latest"
    )
    return re.sub(r"[^A-Za-z0-9_.-]+", "_", str(raw))[:120] or "latest"


def now_iso() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat()


def write_json(path: Path, data: dict[str, Any]) -> None:
    path.write_text(json.dumps(data, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def snapshot_specs(repo: Path) -> dict[str, str]:
    specs = repo / ".specs"
    if not specs.is_dir():
        return {}
    files: dict[str, str] = {}
    for path in sorted(specs.rglob("*")):
        if path.is_file():
            rel = path.relative_to(repo).as_posix()
            files[rel] = sha256(path)
    return files


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def touched_spec_features(repo: Path, state_dir: Path, payload: dict[str, Any]) -> list[str]:
    forced = os.environ.get("SPEC_SELF_EVAL_TOUCHED_FEATURES")
    if forced:
        return sorted({item.strip() for item in forced.split(",") if item.strip()})

    before = load_snapshot(state_dir, payload)
    if before is not None:
        return features_from_snapshot_diff(before, snapshot_specs(repo))

    from_transcript = features_from_transcript(repo, payload)
    if from_transcript:
        return sorted(from_transcript)

    return []


def load_snapshot(state_dir: Path, payload: dict[str, Any]) -> dict[str, str] | None:
    candidates = [state_dir / f"{state_key(payload)}.json", state_dir / "latest.json"]
    for path in candidates:
        if path.is_file():
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
            except json.JSONDecodeError:
                continue
            files = data.get("files")
            if isinstance(files, dict):
                return {str(k): str(v) for k, v in files.items()}
    return None


def features_from_snapshot_diff(before: dict[str, str], after: dict[str, str]) -> list[str]:
    changed = {path for path, digest in after.items() if before.get(path) != digest}
    changed.update(path for path in before if path not in after)
    return sorted({feature_from_path(path) for path in changed if feature_from_path(path)})


def features_from_transcript(repo: Path, payload: dict[str, Any]) -> set[str]:
    transcript = payload.get("transcript_path")
    turn_id = payload.get("turn_id")
    if not transcript:
        return set()
    transcript_path = Path(str(transcript)).expanduser()
    if not transcript_path.is_file():
        return set()

    features: set[str] = set()
    for line in transcript_path.read_text(encoding="utf-8", errors="replace").splitlines():
        try:
            item = json.loads(line)
        except json.JSONDecodeError:
            continue
        event_payload = item.get("payload") if isinstance(item, dict) else None
        if not isinstance(event_payload, dict):
            continue
        if turn_id and event_payload.get("turn_id") not in (None, turn_id):
            continue
        collect_features_from_event(repo, event_payload, features)
    return features


def collect_features_from_event(repo: Path, event: dict[str, Any], features: set[str]) -> None:
    changes = event.get("changes")
    if isinstance(changes, dict):
        for path in changes:
            add_feature(path, features)

    parsed = event.get("parsed_cmd")
    if isinstance(parsed, list):
        for entry in parsed:
            if not isinstance(entry, dict):
                continue
            for key in ("path", "name"):
                value = entry.get(key)
                if value:
                    add_feature(value, features)

    if event.get("name") == "apply_patch":
        patch_input = event.get("input")
        if isinstance(patch_input, str):
            for match in re.finditer(r"^\*\*\* (?:Add|Update|Delete) File: (.+)$", patch_input, re.M):
                add_feature(match.group(1), features)


def add_feature(path_like: Any, features: set[str]) -> None:
    feature = feature_from_path(str(path_like))
    if feature:
        features.add(feature)


def feature_from_path(path: str) -> str | None:
    normalized = path.replace("\\", "/")
    match = re.search(r"(?:^|/)\.specs/([^/]+)/", normalized)
    if not match:
        return None
    feature = match.group(1)
    if feature.startswith("_"):
        return None
    return feature


def run_spec_self_eval(repo: Path, feature: str) -> Path:
    override = os.environ.get("SPEC_SELF_EVAL_REPORT_PATH")
    if override:
        path = Path(override)
        return path if path.is_absolute() else repo / path

    report = repo / ".specs" / feature / f"eval-report-{dt.date.today().isoformat()}.md"
    if os.environ.get("SPEC_SELF_EVAL_SKIP_CODEX") == "1":
        if report.is_file():
            return report
        raise_block(f"spec-self-eval report does not exist: {report.relative_to(repo)}")

    prompt = (
        "Use the spec-self-eval skill. Run it on `.specs/{feature}` in this repo. "
        "Read `.codex/skills/spec-self-eval/SKILL.md` if needed. "
        "Do not edit requirements.md, design.md, tasks.md, or AGENTS.md. "
        "Save the report to `.specs/{feature}/eval-report-{date}.md`; "
        "if that file already exists, append this check result after a separator "
        "instead of overwriting it. "
        "Return only the saved report path and a short PASS/WEAK/FAIL summary."
    ).format(feature=feature, date=dt.date.today().isoformat())

    codex = shlex.split(os.environ.get("SPEC_SELF_EVAL_CODEX_CMD", "codex"))
    cmd = [
        *codex,
        "exec",
        "--cd",
        str(repo),
        "--disable",
        "hooks",
        "--sandbox",
        "workspace-write",
        prompt,
    ]
    proc = subprocess.run(
        cmd,
        cwd=repo,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=int(os.environ.get("SPEC_SELF_EVAL_TIMEOUT_SECONDS", "600")),
        check=False,
    )
    if proc.returncode != 0:
        tail = (proc.stderr or proc.stdout)[-2000:]
        raise_block(
            "spec-self-eval could not run for "
            f"`.specs/{feature}`. Fix the hook failure before closing the turn.\n{tail}"
        )
    if not report.is_file():
        raise_block(
            f"spec-self-eval finished but did not create `{report.relative_to(repo)}`."
        )
    return report


def parse_failures(report: Path, feature: str) -> list[str]:
    if not report.is_file():
        return [f".specs/{feature}: missing eval report `{report}`"]

    lines = latest_eval_block(report.read_text(encoding="utf-8", errors="replace").splitlines())
    failures: list[str] = []
    in_fail_section = False
    section_items: list[str] = []

    for line in lines:
        stripped = line.strip()

        if stripped.startswith("#"):
            if in_fail_section:
                failures.extend(normalize_fail_section(section_items, feature))
            in_fail_section = stripped.lower() == "### fail"
            section_items = []
            continue

        if in_fail_section:
            section_items.append(stripped)

        if "[FAIL]" in stripped:
            failures.append(f".specs/{feature}: {stripped}")
            continue

        table = re.match(r"^\|\s*FAIL\s*\|\s*([^|]*)\|\s*([^|]*)\|", stripped)
        if table:
            area = table.group(1).strip()
            finding = table.group(2).strip()
            failures.append(f".specs/{feature}: {area} - {finding}")

    if in_fail_section:
        failures.extend(normalize_fail_section(section_items, feature))

    return dedupe(failures)


def latest_eval_block(lines: list[str]) -> list[str]:
    for index in range(len(lines) - 1, -1, -1):
        if lines[index].startswith("# Spec Self-Eval:"):
            return lines[index:]
    return lines


def normalize_fail_section(lines: list[str], feature: str) -> list[str]:
    content = [line for line in lines if line and not set(line) <= {"-"}]
    if not content:
        return []
    lowered = " ".join(content).lower().strip()
    if lowered in {"none.", "none", "- none.", "- none"}:
        return []
    return [f".specs/{feature}: FAIL section contains unresolved items"]


def dedupe(items: list[str]) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for item in items:
        if item not in seen:
            result.append(item)
            seen.add(item)
    return result


def raise_block(reason: str) -> None:
    print(json.dumps({"decision": "block", "reason": reason}))
    raise SystemExit(0)


if __name__ == "__main__":
    raise SystemExit(main())

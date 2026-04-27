#!/usr/bin/env sh
set -eu

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-audit-service-postgres-run}"
POSTGRES_PORT="${POSTGRES_PORT:-55433}"
POSTGRES_DB="${POSTGRES_DB:-audit_log}"
POSTGRES_USER="${POSTGRES_USER:-audit}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-audit}"

if ! docker ps --format '{{.Names}}' | grep -qx "$POSTGRES_CONTAINER"; then
  if docker ps -a --format '{{.Names}}' | grep -qx "$POSTGRES_CONTAINER"; then
    docker rm "$POSTGRES_CONTAINER" >/dev/null
  fi

  docker run --rm -d \
    --name "$POSTGRES_CONTAINER" \
    --network host \
    -e POSTGRES_DB="$POSTGRES_DB" \
    -e POSTGRES_USER="$POSTGRES_USER" \
    -e POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
    postgres:16 postgres -c port="$POSTGRES_PORT" >/dev/null
fi

export DATABASE_URL="jdbc:postgresql://127.0.0.1:${POSTGRES_PORT}/${POSTGRES_DB}?sslmode=disable"
export DATABASE_USERNAME="$POSTGRES_USER"
export DATABASE_PASSWORD="$POSTGRES_PASSWORD"

exec ./gradlew bootRun


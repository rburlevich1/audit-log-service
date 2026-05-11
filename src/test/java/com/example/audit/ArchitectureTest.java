package com.example.audit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.audit.event.AuditEvent;
import com.example.audit.event.AuditEventController;
import com.example.audit.event.AuditEventPageResponse;
import com.example.audit.event.AuditEventResponse;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.lang.reflect.ParameterizedType;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

@AnalyzeClasses(
    packages = "com.example.audit",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
  @ArchTest
  static final ArchRule controllers_do_not_access_repositories =
      noClasses()
          .that()
          .haveSimpleNameEndingWith("Controller")
          .should()
          .dependOnClassesThat()
          .haveSimpleNameEndingWith("Repository");

  @ArchTest
  static final ArchRule repositories_are_only_accessed_by_services =
      classes()
          .that()
          .haveSimpleNameEndingWith("Repository")
          .should()
          .onlyBeAccessed()
          .byClassesThat()
          .haveSimpleNameEndingWith("Service");

  @ArchTest
  static final ArchRule services_do_not_access_controllers =
      noClasses()
          .that()
          .haveSimpleNameEndingWith("Service")
          .should()
          .dependOnClassesThat()
          .haveSimpleNameEndingWith("Controller");

  @ArchTest
  static final ArchRule event_package_does_not_depend_on_retention =
      noClasses()
          .that()
          .resideInAPackage("..event..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..retention..");

  @ArchTest
  static final ArchRule retention_package_does_not_depend_on_event =
      noClasses()
          .that()
          .resideInAPackage("..retention..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..event..");

  @Test
  void queryEndpointDoesNotReturnEntityTypes() {
    var queryEndpoint =
        java.util.Arrays.stream(AuditEventController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(GetMapping.class))
            .findFirst()
            .orElseThrow();

    assertThat(queryEndpoint.getReturnType()).isEqualTo(AuditEventPageResponse.class);
    assertThat(queryEndpoint.getReturnType()).isNotEqualTo(AuditEvent.class);

    var items =
        java.util.Arrays.stream(AuditEventPageResponse.class.getRecordComponents())
            .filter(component -> component.getName().equals("items"))
            .findFirst()
            .orElseThrow();

    assertThat(items.getGenericType()).isInstanceOf(ParameterizedType.class);
    var itemType = ((ParameterizedType) items.getGenericType()).getActualTypeArguments()[0];
    assertThat(itemType).isEqualTo(AuditEventResponse.class);
    assertThat(itemType).isNotEqualTo(AuditEvent.class);
  }
}

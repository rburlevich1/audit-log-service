package com.example.audit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.example.audit", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    @ArchTest
    static final ArchRule controllers_do_not_access_repositories =
            noClasses()
                    .that().haveSimpleNameEndingWith("Controller")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule repositories_are_only_accessed_by_services =
            classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .should().onlyBeAccessed().byClassesThat().haveSimpleNameEndingWith("Service");

    @ArchTest
    static final ArchRule services_do_not_access_controllers =
            noClasses()
                    .that().haveSimpleNameEndingWith("Service")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule event_package_does_not_depend_on_retention =
            noClasses()
                    .that().resideInAPackage("..event..")
                    .should().dependOnClassesThat().resideInAPackage("..retention..");

    @ArchTest
    static final ArchRule retention_package_does_not_depend_on_event =
            noClasses()
                    .that().resideInAPackage("..retention..")
                    .should().dependOnClassesThat().resideInAPackage("..event..");
}


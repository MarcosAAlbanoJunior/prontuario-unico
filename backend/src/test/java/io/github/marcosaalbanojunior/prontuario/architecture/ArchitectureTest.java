package io.github.marcosaalbanojunior.prontuario.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

@DisabledInNativeImage
@DisplayName("Architecture rules (AGENTS.md section 4)")
class ArchitectureTest {

    private static final String ROOT = "io.github.marcosaalbanojunior.prontuario";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Nested
    @DisplayName("dependencies between layers")
    class Layers {

        @Test
        @DisplayName("domain <- application <- adapter <- config, never the other way")
        void should_respectLayerDirection_when_classesDependOnEachOther() {
            layeredArchitecture().consideringOnlyDependenciesInLayers()
                    .layer("Domain").definedBy(ROOT + ".domain..")
                    .layer("Application").definedBy(ROOT + ".application..")
                    .layer("Adapter").definedBy(ROOT + ".adapter..")
                    .layer("Config").definedBy(ROOT + ".config..")
                    .whereLayer("Config").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Adapter").mayOnlyBeAccessedByLayers("Config")
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter", "Config")
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter", "Config")
                    .check(CLASSES);
        }

        @Test
        @DisplayName("domain depends only on the JDK and itself")
        void should_dependOnlyOnJdk_when_inDomain() {
            classes().that().resideInAPackage(ROOT + ".domain..")
                    .should().onlyDependOnClassesThat().resideInAnyPackage("java..", ROOT + ".domain..")
                    .check(CLASSES);
        }

        @Test
        @DisplayName("domain models do not depend on domain services")
        void should_notUseServices_when_inDomainModel() {
            noClasses().that().resideInAPackage(ROOT + ".domain.model..")
                    .should().dependOnClassesThat().resideInAPackage(ROOT + ".domain.service..")
                    .check(CLASSES);
        }

        @Test
        @DisplayName("application is free of Spring, Jakarta and Jackson")
        void should_beFrameworkFree_when_inApplication() {
            noClasses().that().resideInAPackage(ROOT + ".application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..", "jakarta..", "tools.jackson..", "com.fasterxml..")
                    .check(CLASSES);
        }

        @Test
        @DisplayName("adapters never call other adapters")
        void should_beIndependent_when_inDifferentAdapters() {
            slices().matching(ROOT + ".adapter.(*).(*)..").should().notDependOnEachOther().check(CLASSES);
        }
    }

    @Nested
    @DisplayName("naming (AGENTS.md section 4.3)")
    class Naming {

        @Test
        @DisplayName("inbound ports are *UseCase interfaces")
        void should_endWithUseCase_when_inboundPortInterface() {
            classes().that().resideInAPackage(ROOT + ".application.port.in..").and().areInterfaces().and(notSealed())
                    .should().haveSimpleNameEndingWith("UseCase")
                    .check(CLASSES);
        }

        @Test
        @DisplayName("outbound ports are *Port interfaces")
        void should_endWithPort_when_outboundPortInterface() {
            classes().that().resideInAPackage(ROOT + ".application.port.out..").and().areInterfaces()
                    .should().haveSimpleNameEndingWith("Port")
                    .check(CLASSES);
        }

        @Test
        @DisplayName("use case implementations are *Service classes in application.usecase")
        void should_endWithService_when_implementingUseCase() {
            classes().that().implement(nameEndingWith("UseCase"))
                    .should().haveSimpleNameEndingWith("Service").andShould().resideInAPackage(ROOT + ".application.usecase..")
                    .check(CLASSES);
        }

        @Test
        @DisplayName("port implementations are *Adapter classes in adapter")
        void should_endWithAdapter_when_implementingPort() {
            classes().that().implement(nameEndingWith("Port"))
                    .should().haveSimpleNameEndingWith("Adapter").andShould().resideInAPackage(ROOT + ".adapter..")
                    .check(CLASSES);
        }
    }

    @Test
    @DisplayName("uses constructor injection only")
    void should_notUseFieldInjection_when_wiringBeans() {
        NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(CLASSES);
    }

    private static DescribedPredicate<JavaClass> notSealed() {
        return DescribedPredicate.describe("are not sealed result types", javaClass -> !javaClass.reflect().isSealed());
    }

    private static DescribedPredicate<JavaClass> nameEndingWith(String suffix) {
        return DescribedPredicate.describe("a type named *" + suffix, javaClass -> javaClass.getSimpleName().endsWith(suffix));
    }
}

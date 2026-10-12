package io.github.marcosaalbanojunior.prontuario.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

// Guards the dependency rules described in CLAUDE.md. Rules allow empty matches
// because most modules have no classes yet.
@DisabledInNativeImage
class ArchitectureTest {

    private static final String ROOT = "io.github.marcosaalbanojunior.prontuario";
    private static final String COMMON = "common";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void domainShouldDependOnlyOnTheJdk() {
        classes().that().resideInAPackage("..domain..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "..domain..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void applicationShouldNotDependOnFrameworksAdaptersOrWiring() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "tools.jackson..", "com.fasterxml..",
                        "..adapter..", "..config..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void inboundAdaptersShouldNotCallOutboundAdapters() {
        noClasses().that().resideInAPackage("..adapter.in..")
                .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void outboundAdaptersShouldNotCallInboundAdapters() {
        noClasses().that().resideInAPackage("..adapter.out..")
                .should().dependOnClassesThat().resideInAPackage("..adapter.in..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void onlyWiringShouldDependOnAdapters() {
        noClasses().that().resideOutsideOfPackages("..adapter..", "..config..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void persistenceRowsShouldNotLeaveTheirAdapter() {
        noClasses().that().resideOutsideOfPackage("..adapter.out.persistence..")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Row")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void modulesShouldTalkOnlyThroughInboundPorts() {
        classes().should(useOtherModulesOnlyThroughInboundPorts())
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void commonShouldNotDependOnFeatureModules() {
        classes().that().resideInAPackage(ROOT + "." + COMMON + "..")
                .should(notDependOnFeatureModules())
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void modulesShouldBeFreeOfCycles() {
        slices().matching(ROOT + ".(*)..")
                .should().beFreeOfCycles()
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void shouldUseConstructorInjection() {
        NO_CLASSES_SHOULD_USE_FIELD_INJECTION.check(CLASSES);
    }

    private static ArchCondition<JavaClass> useOtherModulesOnlyThroughInboundPorts() {
        return new ArchCondition<>("use other modules only through their application.port.in") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                var originModule = moduleOf(origin);
                origin.getDirectDependenciesFromSelf().forEach(dependency -> {
                    var target = dependency.getTargetClass();
                    var targetModule = moduleOf(target);
                    var crossesModules = targetModule.isPresent()
                            && !targetModule.equals(originModule)
                            && !targetModule.get().equals(COMMON);
                    if (crossesModules && !isInboundPort(target, targetModule.get())) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                });
            }
        };
    }

    private static ArchCondition<JavaClass> notDependOnFeatureModules() {
        return new ArchCondition<>("not depend on feature modules") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                origin.getDirectDependenciesFromSelf().forEach(dependency -> {
                    var targetModule = moduleOf(dependency.getTargetClass());
                    if (targetModule.isPresent() && !targetModule.get().equals(COMMON)) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                });
            }
        };
    }

    private static boolean isInboundPort(JavaClass target, String module) {
        var inboundPorts = ROOT + "." + module + ".application.port.in";
        return target.getPackageName().equals(inboundPorts) || target.getPackageName().startsWith(inboundPorts + ".");
    }

    private static Optional<String> moduleOf(JavaClass javaClass) {
        var packageName = javaClass.getPackageName();
        if (!packageName.startsWith(ROOT + ".")) {
            return Optional.empty();
        }
        return Optional.of(packageName.substring(ROOT.length() + 1).split("\\.")[0]);
    }
}

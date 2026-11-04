package co.stockea;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "co.stockea", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    @ArchTest
    static final ArchRule dominio_no_depende_de_nada_externo =
            noClasses().that().resideInAPackage("..dominio..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..aplicacion..", "..infraestructura..", "..interfaces..",
                            "org.springframework..", "jakarta.persistence..", "tools.jackson..", "com.fasterxml.jackson..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule aplicacion_no_depende_de_infraestructura_ni_interfaces =
            noClasses().that().resideInAPackage("..aplicacion..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..infraestructura..", "..interfaces..")
                    .allowEmptyShould(true);
}

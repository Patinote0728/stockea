package co.stockea.cuenta.dominio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    private final PoliticaContrasena policy = new PoliticaContrasena();

    @Test
    void validPasswordHasNoViolations() {
        assertEquals(List.of(), policy.validate(new ContrasenaPlana("Abcdef12345"), "998877", "user@example.com"));
    }

    @Test
    void passwordWithExactlyTenCharactersHasNoViolations() {
        assertEquals(List.of(), policy.validate(new ContrasenaPlana("Abcdef1234"), "998877", "user@example.com"));
    }

    @Test
    void passwordWithNineCharactersIsTooShort() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.DEMASIADO_CORTA),
                policy.validate(new ContrasenaPlana("Abcdef123"), "998877", "user@example.com"));
    }

    @Test
    void detectsDocumentNumberAnywhereInPassword() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.CONTIENE_NUMERO_DOCUMENTO),
                policy.validate(new ContrasenaPlana("My-123456-password"), "123456", "user@example.com"));
    }

    @Test
    void detectsEmailWithoutCaseSensitivity() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.CONTIENE_EMAIL),
                policy.validate(new ContrasenaPlana("UseALICE@EXAMPLE.COM-now"), "998877", "alice@example.com"));
    }

    @Test
    void returnsAllViolations() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.DEMASIADO_CORTA,
                        InfraccionPoliticaContrasenas.CONTIENE_NUMERO_DOCUMENTO,
                        InfraccionPoliticaContrasenas.CONTIENE_EMAIL),
                policy.validate(new ContrasenaPlana("12ab@x.c"), "12", "ab@x.c"));
    }

    @Test
    void acceptsPasswordNotPreviouslyUsedAccordingToIdentityProvider() {
        assertEquals(List.of(), policy.validateWithHistory(
                new ContrasenaPlana("Abcdef1234"), new ContrasenaPlana("Old-pass-123"),
                ResultadoHistorialContrasena.NO_REUTILIZADA, "998877", "user@example.com"));
    }

    @Test
    void rejectsPasswordEqualToCurrentPassword() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.IGUAL_A_LA_ACTUAL),
                policy.validateWithHistory(new ContrasenaPlana("Abcdef1234"), new ContrasenaPlana("Abcdef1234"),
                        ResultadoHistorialContrasena.NO_REUTILIZADA, "998877", "user@example.com"));
    }

    @Test
    void rejectsPasswordReportedAsPreviouslyUsedByIdentityProvider() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.EN_HISTORIAL_DE_CONTRASENAS),
                policy.validateWithHistory(new ContrasenaPlana("Abcdef1234"), new ContrasenaPlana("Old-pass-123"),
                        ResultadoHistorialContrasena.REUTILIZADA, "998877", "user@example.com"));
    }

    @Test
    void accumulatesHistoryAndOtherPasswordViolations() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.DEMASIADO_CORTA,
                        InfraccionPoliticaContrasenas.IGUAL_A_LA_ACTUAL,
                        InfraccionPoliticaContrasenas.EN_HISTORIAL_DE_CONTRASENAS,
                        InfraccionPoliticaContrasenas.CONTIENE_NUMERO_DOCUMENTO),
                policy.validateWithHistory(new ContrasenaPlana("123456789"), new ContrasenaPlana("123456789"),
                        ResultadoHistorialContrasena.REUTILIZADA, "123", "user@example.com"));
    }

    @Test
    void rejectsMissingHistoryContext() {
        ContrasenaPlana password = new ContrasenaPlana("Abcdef1234");
        ContrasenaPlana currentPassword = new ContrasenaPlana("Old-pass-123");

        assertThrows(IllegalArgumentException.class, () -> policy.validateWithHistory(
                password, null, ResultadoHistorialContrasena.NO_REUTILIZADA, "998877", "user@example.com"));
        assertThrows(IllegalArgumentException.class, () -> policy.validateWithHistory(
                password, currentPassword, null, "998877", "user@example.com"));
    }

    @Test
    void countsUnicodeCodePointsInsteadOfUtf16Units() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.DEMASIADO_CORTA),
                policy.validate(new ContrasenaPlana("😀😀😀😀😀😀😀😀😀"), "998877", "user@example.com"));
    }

    @Test
    void rejectsMissingIdentityValues() {
        ContrasenaPlana password = new ContrasenaPlana("Abcdef1234");

        assertThrows(IllegalArgumentException.class, () -> policy.validate(null, "998877", "user@example.com"));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(password, null, "user@example.com"));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(password, " ", "user@example.com"));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(password, "998877", null));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(password, "998877", ""));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(password, "998877", " "));
    }

    @Test
    void returnsAnImmutableViolationList() {
        List<InfraccionPoliticaContrasenas> violations =
                policy.validate(new ContrasenaPlana("short"), "998877", "user@example.com");

        assertThrows(UnsupportedOperationException.class,
                () -> violations.add(InfraccionPoliticaContrasenas.CONTIENE_EMAIL));
    }
}
package co.stockea.cuenta.dominio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void validPasswordHasNoViolations() {
        assertEquals(List.of(), policy.validate(new RawPassword("Abcdef12345"), "998877", "user@example.com"));
    }

    @Test
    void passwordWithExactlyTenCharactersHasNoViolations() {
        assertEquals(List.of(), policy.validate(new RawPassword("Abcdef1234"), "998877", "user@example.com"));
    }

    @Test
    void passwordWithNineCharactersIsTooShort() {
        assertEquals(List.of(PasswordPolicyViolation.TOO_SHORT),
                policy.validate(new RawPassword("Abcdef123"), "998877", "user@example.com"));
    }

    @Test
    void detectsDocumentNumberAnywhereInPassword() {
        assertEquals(List.of(PasswordPolicyViolation.CONTAINS_DOCUMENT_NUMBER),
                policy.validate(new RawPassword("My-123456-password"), "123456", "user@example.com"));
    }

    @Test
    void detectsEmailWithoutCaseSensitivity() {
        assertEquals(List.of(PasswordPolicyViolation.CONTAINS_EMAIL),
                policy.validate(new RawPassword("UseALICE@EXAMPLE.COM-now"), "998877", "alice@example.com"));
    }

    @Test
    void returnsAllViolations() {
        assertEquals(List.of(PasswordPolicyViolation.TOO_SHORT,
                        PasswordPolicyViolation.CONTAINS_DOCUMENT_NUMBER,
                        PasswordPolicyViolation.CONTAINS_EMAIL),
                policy.validate(new RawPassword("12ab@x.c"), "12", "ab@x.c"));
    }

    @Test
    void acceptsPasswordNotPreviouslyUsedAccordingToIdentityProvider() {
        assertEquals(List.of(), policy.validateWithHistory(
                new RawPassword("Abcdef1234"), new RawPassword("Old-pass-123"),
                PasswordHistoryResult.NOT_REUSED, "998877", "user@example.com"));
    }

    @Test
    void rejectsPasswordEqualToCurrentPassword() {
        assertEquals(List.of(PasswordPolicyViolation.SAME_AS_CURRENT_PASSWORD),
                policy.validateWithHistory(new RawPassword("Abcdef1234"), new RawPassword("Abcdef1234"),
                        PasswordHistoryResult.NOT_REUSED, "998877", "user@example.com"));
    }

    @Test
    void rejectsPasswordReportedAsPreviouslyUsedByIdentityProvider() {
        assertEquals(List.of(PasswordPolicyViolation.IN_PASSWORD_HISTORY),
                policy.validateWithHistory(new RawPassword("Abcdef1234"), new RawPassword("Old-pass-123"),
                        PasswordHistoryResult.REUSED, "998877", "user@example.com"));
    }

    @Test
    void accumulatesHistoryAndOtherPasswordViolations() {
        assertEquals(List.of(PasswordPolicyViolation.TOO_SHORT,
                        PasswordPolicyViolation.SAME_AS_CURRENT_PASSWORD,
                        PasswordPolicyViolation.IN_PASSWORD_HISTORY,
                        PasswordPolicyViolation.CONTAINS_DOCUMENT_NUMBER),
                policy.validateWithHistory(new RawPassword("123456789"), new RawPassword("123456789"),
                        PasswordHistoryResult.REUSED, "123", "user@example.com"));
    }

    @Test
    void rejectsMissingHistoryContext() {
        RawPassword password = new RawPassword("Abcdef1234");
        RawPassword currentPassword = new RawPassword("Old-pass-123");

        assertThrows(IllegalArgumentException.class, () -> policy.validateWithHistory(
                password, null, PasswordHistoryResult.NOT_REUSED, "998877", "user@example.com"));
        assertThrows(IllegalArgumentException.class, () -> policy.validateWithHistory(
                password, currentPassword, null, "998877", "user@example.com"));
    }

    @Test
    void countsUnicodeCodePointsInsteadOfUtf16Units() {
        assertEquals(List.of(PasswordPolicyViolation.TOO_SHORT),
                policy.validate(new RawPassword("😀😀😀😀😀😀😀😀😀"), "998877", "user@example.com"));
    }

    @Test
    void rejectsMissingIdentityValues() {
        RawPassword password = new RawPassword("Abcdef1234");

        assertThrows(IllegalArgumentException.class, () -> policy.validate(null, "998877", "user@example.com"));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(password, null, "user@example.com"));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(password, " ", "user@example.com"));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(password, "998877", null));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(password, "998877", ""));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(password, "998877", " "));
    }

    @Test
    void returnsAnImmutableViolationList() {
        List<PasswordPolicyViolation> violations =
                policy.validate(new RawPassword("short"), "998877", "user@example.com");

        assertThrows(UnsupportedOperationException.class,
                () -> violations.add(PasswordPolicyViolation.CONTAINS_EMAIL));
    }
}
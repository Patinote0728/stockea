package co.stockea.cuenta.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PasswordPolicy {

    private static final int MINIMUM_LENGTH = 10;

    public List<PasswordPolicyViolation> validate(RawPassword password, String documentNumber, String email) {
        return validate(password, null, null, documentNumber, email, false);
    }

    public List<PasswordPolicyViolation> validateWithHistory(RawPassword password, RawPassword currentPassword,
                                                              PasswordHistoryResult historyResult,
                                                              String documentNumber, String email) {
        if (currentPassword == null) {
            throw new IllegalArgumentException("La contraseña actual no puede ser nula");
        }
        if (historyResult == null) {
            throw new IllegalArgumentException("El resultado del historial de contraseñas no puede ser nulo");
        }
        return validate(password, currentPassword, historyResult, documentNumber, email, true);
    }

    private List<PasswordPolicyViolation> validate(RawPassword password, RawPassword currentPassword,
                                                    PasswordHistoryResult historyResult, String documentNumber,
                                                    String email, boolean includeHistory) {
        if (password == null) {
            throw new IllegalArgumentException("La contraseña no puede ser nula");
        }
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new IllegalArgumentException("El numero de documento no puede ser nulo ni vacío");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email no puede ser nulo ni vacío");
        }

        String value = password.value();
        List<PasswordPolicyViolation> violations = new ArrayList<>();
        if (value.codePointCount(0, value.length()) < MINIMUM_LENGTH) {
            violations.add(PasswordPolicyViolation.TOO_SHORT);
        }
        if (includeHistory && value.equals(currentPassword.value())) {
            violations.add(PasswordPolicyViolation.SAME_AS_CURRENT_PASSWORD);
        }
        if (includeHistory && historyResult == PasswordHistoryResult.REUSED) {
            violations.add(PasswordPolicyViolation.IN_PASSWORD_HISTORY);
        }
        if (value.contains(documentNumber)) {
            violations.add(PasswordPolicyViolation.CONTAINS_DOCUMENT_NUMBER);
        }
        if (value.toLowerCase(Locale.ROOT).contains(email.toLowerCase(Locale.ROOT))) {
            violations.add(PasswordPolicyViolation.CONTAINS_EMAIL);
        }
        return List.copyOf(violations);
    }
}
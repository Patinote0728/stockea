package co.stockea.cuenta.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PoliticaContrasena {

    private static final int MINIMUM_LENGTH = 10;

    public List<InfraccionPoliticaContrasenas> validate(ContrasenaPlana password, String documentNumber, String email) {
        return validate(password, null, null, documentNumber, email, false);
    }

    public List<InfraccionPoliticaContrasenas> validateWithHistory(ContrasenaPlana password, ContrasenaPlana currentPassword,
                                                                   ResultadoHistorialContrasena historyResult,
                                                                   String documentNumber, String email) {
        if (currentPassword == null) {
            throw new IllegalArgumentException("La contraseña actual no puede ser nula");
        }
        if (historyResult == null) {
            throw new IllegalArgumentException("El resultado del historial de contraseñas no puede ser nulo");
        }
        return validate(password, currentPassword, historyResult, documentNumber, email, true);
    }

    private List<InfraccionPoliticaContrasenas> validate(ContrasenaPlana password, ContrasenaPlana currentPassword,
                                                         ResultadoHistorialContrasena historyResult, String documentNumber,
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
        List<InfraccionPoliticaContrasenas> violations = new ArrayList<>();
        if (value.codePointCount(0, value.length()) < MINIMUM_LENGTH) {
            violations.add(InfraccionPoliticaContrasenas.DEMASIADO_CORTA);
        }
        if (includeHistory && value.equals(currentPassword.value())) {
            violations.add(InfraccionPoliticaContrasenas.IGUAL_A_LA_ACTUAL);
        }
        if (includeHistory && historyResult == ResultadoHistorialContrasena.REUTILIZADA) {
            violations.add(InfraccionPoliticaContrasenas.EN_HISTORIAL_DE_CONTRASENAS);
        }
        if (value.contains(documentNumber)) {
            violations.add(InfraccionPoliticaContrasenas.CONTIENE_NUMERO_DOCUMENTO);
        }
        if (value.toLowerCase(Locale.ROOT).contains(email.toLowerCase(Locale.ROOT))) {
            violations.add(InfraccionPoliticaContrasenas.CONTIENE_EMAIL);
        }
        return List.copyOf(violations);
    }
}
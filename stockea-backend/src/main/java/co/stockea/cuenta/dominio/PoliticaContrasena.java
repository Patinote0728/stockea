package co.stockea.cuenta.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PoliticaContrasena {

    private static final int LONGITUD_MINIMA = 10;

    public List<InfraccionPoliticaContrasenas> validar(ContrasenaPlana contrasena, String numeroDocumento, String correo) {
        return validar(contrasena, null, null, numeroDocumento, correo, false);
    }

    public List<InfraccionPoliticaContrasenas> validarConHistorial(ContrasenaPlana contrasena, ContrasenaPlana contrasenaActual,
                                                                   ResultadoHistorialContrasena resultadoHistorial,
                                                                   String numeroDocumento, String correo) {
        if (contrasenaActual == null) {
            throw new IllegalArgumentException("La contraseña actual no puede ser nula");
        }
        if (resultadoHistorial == null) {
            throw new IllegalArgumentException("El resultado del historial de contraseñas no puede ser nulo");
        }
        return validar(contrasena, contrasenaActual, resultadoHistorial, numeroDocumento, correo, true);
    }

    private List<InfraccionPoliticaContrasenas> validar(ContrasenaPlana contrasena, ContrasenaPlana contrasenaActual,
                                                         ResultadoHistorialContrasena resultadoHistorial, String numeroDocumento,
                                                         String correo, boolean incluirHistorial) {
        if (contrasena == null) {
            throw new IllegalArgumentException("La contraseña no puede ser nula");
        }
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            throw new IllegalArgumentException("El numero de documento no puede ser nulo ni vacío");
        }
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("El correo no puede ser nulo ni vacío");
        }

        String valor = contrasena.valor();
        List<InfraccionPoliticaContrasenas> infracciones = new ArrayList<>();
        if (valor.codePointCount(0, valor.length()) < LONGITUD_MINIMA) {
            infracciones.add(InfraccionPoliticaContrasenas.DEMASIADO_CORTA);
        }
        if (incluirHistorial && valor.equals(contrasenaActual.valor())) {
            infracciones.add(InfraccionPoliticaContrasenas.IGUAL_A_LA_ACTUAL);
        }
        if (incluirHistorial && resultadoHistorial == ResultadoHistorialContrasena.REUTILIZADA) {
            infracciones.add(InfraccionPoliticaContrasenas.EN_HISTORIAL_DE_CONTRASENAS);
        }
        if (valor.contains(numeroDocumento)) {
            infracciones.add(InfraccionPoliticaContrasenas.CONTIENE_NUMERO_DOCUMENTO);
        }
        if (valor.toLowerCase(Locale.ROOT).contains(correo.toLowerCase(Locale.ROOT))) {
            infracciones.add(InfraccionPoliticaContrasenas.CONTIENE_CORREO);
        }
        return List.copyOf(infracciones);
    }
}

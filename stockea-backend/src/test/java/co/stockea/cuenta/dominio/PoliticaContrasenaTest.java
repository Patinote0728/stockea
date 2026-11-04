package co.stockea.cuenta.dominio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PoliticaContrasenaTest {

    private final PoliticaContrasena politica = new PoliticaContrasena();

    @Test
    void contrasenaValidaNoTieneInfracciones() {
        assertEquals(List.of(), politica.validar(new ContrasenaPlana("Abcdef12345"), "998877", "user@example.com"));
    }

    @Test
    void contrasenaConDiezCaracteresEsValida() {
        assertEquals(List.of(), politica.validar(new ContrasenaPlana("Abcdef1234"), "998877", "user@example.com"));
    }

    @Test
    void contrasenaConNueveCaracteresEsDemasiadoCorta() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.DEMASIADO_CORTA),
                politica.validar(new ContrasenaPlana("Abcdef123"), "998877", "user@example.com"));
    }

    @Test
    void detectaNumeroDocumentoEnCualquierParteDeLaContrasena() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.CONTIENE_NUMERO_DOCUMENTO),
                politica.validar(new ContrasenaPlana("My-123456-password"), "123456", "user@example.com"));
    }

    @Test
    void detectaCorreoSinDistinguirMayusculas() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.CONTIENE_CORREO),
                politica.validar(new ContrasenaPlana("UseALICE@EXAMPLE.COM-now"), "998877", "alice@example.com"));
    }

    @Test
    void devuelveTodasLasInfracciones() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.DEMASIADO_CORTA,
                        InfraccionPoliticaContrasenas.CONTIENE_NUMERO_DOCUMENTO,
                InfraccionPoliticaContrasenas.CONTIENE_CORREO),
                politica.validar(new ContrasenaPlana("12ab@x.c"), "12", "ab@x.c"));
    }

    @Test
    void aceptaContrasenaNoReutilizadaSegunElProveedorDeIdentidad() {
        assertEquals(List.of(), politica.validarConHistorial(
                new ContrasenaPlana("Abcdef1234"), new ContrasenaPlana("Old-pass-123"),
                ResultadoHistorialContrasena.NO_REUTILIZADA, "998877", "user@example.com"));
    }

    @Test
    void rechazaContrasenaIgualALaActual() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.IGUAL_A_LA_ACTUAL),
                politica.validarConHistorial(new ContrasenaPlana("Abcdef1234"), new ContrasenaPlana("Abcdef1234"),
                        ResultadoHistorialContrasena.NO_REUTILIZADA, "998877", "user@example.com"));
    }

    @Test
    void rechazaContrasenaReportadaComoReutilizadaPorElProveedorDeIdentidad() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.EN_HISTORIAL_DE_CONTRASENAS),
                politica.validarConHistorial(new ContrasenaPlana("Abcdef1234"), new ContrasenaPlana("Old-pass-123"),
                        ResultadoHistorialContrasena.REUTILIZADA, "998877", "user@example.com"));
    }

    @Test
    void acumulaInfraccionesDelHistorialYDeLasDemasReglas() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.DEMASIADO_CORTA,
                        InfraccionPoliticaContrasenas.IGUAL_A_LA_ACTUAL,
                        InfraccionPoliticaContrasenas.EN_HISTORIAL_DE_CONTRASENAS,
                        InfraccionPoliticaContrasenas.CONTIENE_NUMERO_DOCUMENTO),
                politica.validarConHistorial(new ContrasenaPlana("123456789"), new ContrasenaPlana("123456789"),
                        ResultadoHistorialContrasena.REUTILIZADA, "123", "user@example.com"));
    }

    @Test
    void rechazaContextoDeHistorialIncompleto() {
        ContrasenaPlana contrasena = new ContrasenaPlana("Abcdef1234");
        ContrasenaPlana contrasenaActual = new ContrasenaPlana("Old-pass-123");

        assertThrows(IllegalArgumentException.class, () -> politica.validarConHistorial(
                contrasena, null, ResultadoHistorialContrasena.NO_REUTILIZADA, "998877", "user@example.com"));
        assertThrows(IllegalArgumentException.class, () -> politica.validarConHistorial(
                contrasena, contrasenaActual, null, "998877", "user@example.com"));
    }

    @Test
    void cuentaPuntosDeCodigoUnicodeEnLugarDeUnidadesUtf16() {
        assertEquals(List.of(InfraccionPoliticaContrasenas.DEMASIADO_CORTA),
                politica.validar(new ContrasenaPlana("😀😀😀😀😀😀😀😀😀"), "998877", "user@example.com"));
    }

    @Test
    void rechazaDatosDeIdentidadAusentes() {
        ContrasenaPlana contrasena = new ContrasenaPlana("Abcdef1234");

        assertThrows(IllegalArgumentException.class, () -> politica.validar(null, "998877", "user@example.com"));
        assertThrows(IllegalArgumentException.class, () -> politica.validar(contrasena, null, "user@example.com"));
        assertThrows(IllegalArgumentException.class, () -> politica.validar(contrasena, " ", "user@example.com"));
        assertThrows(IllegalArgumentException.class, () -> politica.validar(contrasena, "998877", null));
        assertThrows(IllegalArgumentException.class, () -> politica.validar(contrasena, "998877", ""));
        assertThrows(IllegalArgumentException.class, () -> politica.validar(contrasena, "998877", " "));
    }

    @Test
    void devuelveUnaListaDeInfraccionesInmutable() {
        List<InfraccionPoliticaContrasenas> infracciones =
                politica.validar(new ContrasenaPlana("short"), "998877", "user@example.com");

        assertThrows(UnsupportedOperationException.class,
                () -> infracciones.add(InfraccionPoliticaContrasenas.CONTIENE_CORREO));
    }
}

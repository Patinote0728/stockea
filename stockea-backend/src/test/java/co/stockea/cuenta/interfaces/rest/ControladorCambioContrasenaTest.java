package co.stockea.cuenta.interfaces.rest;

import co.stockea.cuenta.aplicacion.CambiarContrasenaCasoDeUso;
import co.stockea.cuenta.aplicacion.modelo.CambiarContrasenaSolicitud;
import co.stockea.cuenta.aplicacion.puerto.PuertoAuditoria;
import co.stockea.cuenta.dominio.excepcion.ContrasenaActualInvalidaExcepcion;
import co.stockea.cuenta.interfaces.seguridad.LimitadorIntentosCambioContrasena;
import co.stockea.cuenta.interfaces.seguridad.LimitadorIntentosCambioContrasena.LimiteIntentosAlcanzadoException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ControladorCambioContrasenaTest {

    @Test
    void construyeLaSolicitudConIdentidadFirmadaYNoConIdentidadDelCliente() {
        CambiarContrasenaCasoDeUso casoDeUso = mock(CambiarContrasenaCasoDeUso.class);
        PuertoAuditoria auditoria = mock(PuertoAuditoria.class);
        var controlador = new ControladorCambioContrasena(
                casoDeUso, new LimitadorIntentosCambioContrasena(), auditoria);
        SolicitudCambioContrasenaHttp cuerpo = new SolicitudCambioContrasenaHttp(
                "actual-secreta", "nueva-secreta-valida");

        var respuesta = controlador.cambiar(jwt("usuario-123"), cuerpo, peticion());

        assertEquals(HttpStatus.NO_CONTENT, respuesta.getStatusCode());
        assertEquals("no-store", respuesta.getHeaders().getCacheControl());
        var capturador = org.mockito.ArgumentCaptor.forClass(CambiarContrasenaSolicitud.class);
        verify(casoDeUso).ejecutar(capturador.capture());
        CambiarContrasenaSolicitud solicitud = capturador.getValue();
        assertEquals("usuario-123", solicitud.idUsuario());
        assertEquals("CC", solicitud.tipoDocumento());
        assertEquals("991122", solicitud.numeroDocumento());
        assertEquals("persona@example.com", solicitud.correo());
        assertEquals("192.0.2.10", solicitud.origenSolicitud());
        assertEquals("actual-secreta", solicitud.contrasenaActual().valor());
        assertEquals("nueva-secreta-valida", solicitud.contrasenaNueva().valor());
        assertFalse(cuerpo.toString().contains("actual-secreta"));
    }

    @Test
    void bloqueaElSextoFalloSinInvocarElCasoDeUsoYAuditaElBloqueo() {
        CambiarContrasenaCasoDeUso casoDeUso = mock(CambiarContrasenaCasoDeUso.class);
        PuertoAuditoria auditoria = mock(PuertoAuditoria.class);
        doThrow(new ContrasenaActualInvalidaExcepcion()).when(casoDeUso).ejecutar(any());
        var controlador = new ControladorCambioContrasena(
                casoDeUso, new LimitadorIntentosCambioContrasena(), auditoria);

        for (int intento = 0; intento < 5; intento++) {
            assertThrows(ContrasenaActualInvalidaExcepcion.class,
                    () -> controlador.cambiar(jwt("usuario-123"), solicitudHttp(), peticion()));
        }
        assertThrows(LimiteIntentosAlcanzadoException.class,
                () -> controlador.cambiar(jwt("usuario-123"), solicitudHttp(), peticion()));

        verify(casoDeUso, times(5)).ejecutar(any());
        verify(auditoria).registrarIntentoCambioContrasena("usuario-123", "192.0.2.10", false);
        verifyNoMoreInteractions(auditoria);
    }

    private SolicitudCambioContrasenaHttp solicitudHttp() {
        return new SolicitudCambioContrasenaHttp("actual-secreta", "nueva-secreta-valida");
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token-de-prueba")
                .header("alg", "RS256")
                .subject(subject)
                .claim("preferred_username", "CC-991122")
                .claim("email", "persona@example.com")
                .build();
    }

    private HttpServletRequest peticion() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.setRemoteAddr("192.0.2.10");
        return peticion;
    }
}

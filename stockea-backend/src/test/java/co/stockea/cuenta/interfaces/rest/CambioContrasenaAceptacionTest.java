package co.stockea.cuenta.interfaces.rest;

import co.stockea.cuenta.aplicacion.CambiarContrasenaCasoDeUso;
import co.stockea.cuenta.aplicacion.puerto.PuertoAuditoria;
import co.stockea.cuenta.dominio.excepcion.ContrasenaActualInvalidaExcepcion;
import co.stockea.cuenta.interfaces.seguridad.ConfiguracionSeguridad;
import co.stockea.cuenta.interfaces.seguridad.LimitadorIntentosCambioContrasena;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ControladorCambioContrasena.class)
@Import({ConfiguracionSeguridad.class, LimitadorIntentosCambioContrasena.class})
class CambioContrasenaAceptacionTest {

    private static final String CUERPO_VALIDO = """
            {"contrasenaActual":"actual-secreta","contrasenaNueva":"nueva-secreta-segura"}
            """;

    @Autowired
    private MockMvc clienteHttp;

    @MockitoBean
    private CambiarContrasenaCasoDeUso casoDeUso;

    @MockitoBean
    private PuertoAuditoria auditoria;

    @Test
    void rechazaLaSolicitudSinTokenAntesDeEjecutarElCasoDeUso() throws Exception {
        clienteHttp.perform(post(ControladorCambioContrasena.RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(casoDeUso, auditoria);
    }

    @Test
    void aceptaLaSolicitudAutenticadaYDevuelveRespuestaNoAlmacenable() throws Exception {
        clienteHttp.perform(post(ControladorCambioContrasena.RUTA)
                        .with(jwt().jwt(token -> token
                                .subject("usuario-aceptacion")
                                .claim("preferred_username", "CC-991122")
                                .claim("email", "persona@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Pragma", "no-cache"));

        verify(casoDeUso).ejecutar(any());
    }

    @Test
    void bloqueaYAuditaElSextoFalloSinConsultarDeNuevoAlProveedor() throws Exception {
        doThrow(new ContrasenaActualInvalidaExcepcion())
                .when(casoDeUso).ejecutar(any());

        for (int intento = 0; intento < 5; intento++) {
            clienteHttp.perform(post(ControladorCambioContrasena.RUTA)
                            .with(jwt().jwt(token -> token
                                    .subject("usuario-limite-aceptacion")
                                    .claim("preferred_username", "CC-991122")
                                    .claim("email", "persona@example.com")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json("""
                            {"codigo":"CREDENCIALES_INVALIDAS"}
                            """));
        }

        clienteHttp.perform(post(ControladorCambioContrasena.RUTA)
                        .with(jwt().jwt(token -> token
                                .subject("usuario-limite-aceptacion")
                                .claim("preferred_username", "CC-991122")
                                .claim("email", "persona@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().json("""
                        {"codigo":"LIMITE_INTENTOS"}
                        """));

        verify(casoDeUso, times(5)).ejecutar(any());
        verify(auditoria).registrarIntentoCambioContrasena(
                "usuario-limite-aceptacion", "127.0.0.1", false);
    }
}

package co.stockea.cuenta.infraestructura.idp;

import co.stockea.cuenta.aplicacion.modelo.CredencialesProveedorIdentidad;
import co.stockea.cuenta.dominio.ContrasenaPlana;
import co.stockea.cuenta.dominio.InfraccionPoliticaContrasenas;
import co.stockea.cuenta.dominio.excepcion.PoliticaContrasenaIncumplidaExcepcion;
import co.stockea.cuenta.dominio.excepcion.ProveedorIdentidadNoDisponibleExcepcion;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptadorKeycloakProveedorIdentidadTest {

    private static final CredencialesProveedorIdentidad CREDENCIALES =
            new CredencialesProveedorIdentidad("secreto-verificador", "secreto-administrador");

    private final List<String> solicitudes = new CopyOnWriteArrayList<>();
    private final List<String> cuerpos = new CopyOnWriteArrayList<>();
    private HttpServer servidor;
    private AdaptadorKeycloakProveedorIdentidad adaptador;
    private volatile int estadoCambioContrasena = 204;
    private volatile int estadoTokenContrasena = 200;

    @BeforeEach
    void iniciarServidorLocal() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", this::responder);
        servidor.start();
        String url = "http://localhost:" + servidor.getAddress().getPort();
        adaptador = new AdaptadorKeycloakProveedorIdentidad(
                RestClient.builder().baseUrl(url).build(), "stockea", "stockea-pwd-verifier", "stockea-admin-api");
    }

    @AfterEach
    void detenerServidorLocal() {
        servidor.stop(0);
    }

    @Test
    void verificaActualizaYCierraSesionesUsandoElMismoTokenAdministrativo() {
        assertTrue(adaptador.verificarContrasenaActual(
                "CC-991122", new ContrasenaPlana("contrasena-actual"), CREDENCIALES));

        adaptador.actualizarContrasena("id-usuario", new ContrasenaPlana("contrasena-nueva"), CREDENCIALES);
        adaptador.cerrarSesiones("id-usuario", CREDENCIALES);

        assertEquals(List.of(
                "/realms/stockea/protocol/openid-connect/token:grant_type=password",
                "/realms/stockea/protocol/openid-connect/token:grant_type=client_credentials",
                "/admin/realms/stockea/users/id-usuario/reset-password:Bearer token-administrativo",
                "/admin/realms/stockea/users/id-usuario/logout:Bearer token-administrativo"), solicitudes);
        assertTrue(cuerpos.get(0).contains("password=contrasena-actual"));
        assertTrue(cuerpos.get(0).contains("client_secret=secreto-verificador"));
        assertTrue(cuerpos.get(2).contains("value\":\"contrasena-nueva"));
    }

    @Test
    void convierteElRechazoDeLaContrasenaActualEnFalso() {
        estadoTokenContrasena = 400;

        assertFalse(adaptador.verificarContrasenaActual(
                "CC-991122", new ContrasenaPlana("incorrecta"), CREDENCIALES));
    }

    @Test
    void convierteElRechazoDeLaPoliticaDelIdpEnUnaInfraccionDeHistorial() {
        estadoCambioContrasena = 400;

        var excepcion = assertThrows(PoliticaContrasenaIncumplidaExcepcion.class,
                () -> adaptador.actualizarContrasena("id-usuario", new ContrasenaPlana("contrasena-nueva"),
                        CREDENCIALES));

        assertEquals(InfraccionPoliticaContrasenas.EN_HISTORIAL_DE_CONTRASENAS,
                excepcion.violaciones().get(0));
        assertFalse(excepcion.getMessage().contains("contrasena-nueva"));
    }

    @Test
    void convierteLosErroresDeDisponibilidadEnUnaExcepcionGenerica() {
        estadoTokenContrasena = 503;

        assertThrows(ProveedorIdentidadNoDisponibleExcepcion.class,
                () -> adaptador.verificarContrasenaActual(
                        "CC-991122", new ContrasenaPlana("contrasena-actual"), CREDENCIALES));
    }

    private void responder(HttpExchange intercambio) throws IOException {
        String ruta = intercambio.getRequestURI().getPath();
        String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        cuerpos.add(cuerpo);
        String autorizado = intercambio.getRequestHeaders().getFirst("Authorization");
        int estado;
        String respuesta = "";

        if (ruta.endsWith("/token")) {
            String formulario = URLDecoder.decode(cuerpo, StandardCharsets.UTF_8);
            if (formulario.contains("grant_type=client_credentials")) {
                solicitudes.add(ruta + ":grant_type=client_credentials");
                estado = 200;
                respuesta = "{\"access_token\":\"token-administrativo\",\"expires_in\":300}";
            } else {
                solicitudes.add(ruta + ":grant_type=password");
                estado = estadoTokenContrasena;
                respuesta = estado == 200 ? "{\"access_token\":\"token-usuario\"}" : "{\"error\":\"invalid_grant\"}";
            }
        } else if (ruta.endsWith("/reset-password")) {
            solicitudes.add(ruta + ":" + autorizado);
            estado = estadoCambioContrasena;
        } else {
            solicitudes.add(ruta + ":" + autorizado);
            estado = 204;
        }
        if (estado == 204) {
            intercambio.sendResponseHeaders(estado, -1);
        } else {
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length);
            intercambio.getResponseBody().write(bytes);
        }
        intercambio.close();
    }
}

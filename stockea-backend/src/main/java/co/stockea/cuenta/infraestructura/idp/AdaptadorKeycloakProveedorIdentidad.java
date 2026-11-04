package co.stockea.cuenta.infraestructura.idp;

import co.stockea.cuenta.aplicacion.modelo.CredencialesProveedorIdentidad;
import co.stockea.cuenta.aplicacion.puerto.PuertoProveedorIdentidad;
import co.stockea.cuenta.dominio.ContrasenaPlana;
import co.stockea.cuenta.dominio.InfraccionPoliticaContrasenas;
import co.stockea.cuenta.dominio.excepcion.PoliticaContrasenaIncumplidaExcepcion;
import co.stockea.cuenta.dominio.excepcion.ProveedorIdentidadNoDisponibleExcepcion;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public final class AdaptadorKeycloakProveedorIdentidad implements PuertoProveedorIdentidad {

    private static final long VIGENCIA_PREDETERMINADA_SEGUNDOS = 60;
    private static final long MARGEN_RENOVACION_SEGUNDOS = 10;

    private final RestClient cliente;
    private final String reino;
    private final String clienteVerificador;
    private final String clienteAdministrativo;
    private final Clock reloj = Clock.systemUTC();
    private volatile TokenAdministrativo tokenEnCache;

    public AdaptadorKeycloakProveedorIdentidad(@Qualifier("clienteProveedorIdentidad") RestClient cliente,
                                               @Value("${stockea.proveedor-identidad.reino}") String reino,
                                               @Value("${stockea.proveedor-identidad.cliente-verificador}") String clienteVerificador,
                                               @Value("${stockea.proveedor-identidad.cliente-administrativo}") String clienteAdministrativo) {
        this.cliente = cliente;
        this.reino = reino;
        this.clienteVerificador = clienteVerificador;
        this.clienteAdministrativo = clienteAdministrativo;
    }

    @Override
    public boolean verificarContrasenaActual(String nombreUsuario, ContrasenaPlana contrasenaActual,
                                             CredencialesProveedorIdentidad credenciales) {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("grant_type", "password");
        formulario.add("client_id", clienteVerificador);
        formulario.add("client_secret", credenciales.secretoClienteVerificador());
        formulario.add("username", nombreUsuario);
        formulario.add("password", contrasenaActual.valor());

        try {
            var respuesta = cliente.post()
                    .uri("/realms/{reino}/protocol/openid-connect/token", reino)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .toEntity(Map.class);
            return respuesta.getStatusCode().is2xxSuccessful();
        } catch (RestClientResponseException excepcion) {
            if (excepcion.getStatusCode().value() == 400) {
                return false;
            }
            throw new ProveedorIdentidadNoDisponibleExcepcion();
        } catch (RestClientException excepcion) {
            throw new ProveedorIdentidadNoDisponibleExcepcion();
        }
    }

    @Override
    public void actualizarContrasena(String idUsuario, ContrasenaPlana contrasenaNueva,
                                     CredencialesProveedorIdentidad credenciales) {
        String token = obtenerTokenAdministrativo(credenciales);
        Map<String, Object> credencial = Map.of(
                "type", "password",
                "value", contrasenaNueva.valor(),
                "temporary", false);
        try {
            cliente.put()
                    .uri("/admin/realms/{reino}/users/{idUsuario}/reset-password", reino, idUsuario)
                    .headers(cabeceras -> cabeceras.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(credencial)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException excepcion) {
            int estado = excepcion.getStatusCode().value();
            if (estado == 400 || estado == 409) {
                throw new PoliticaContrasenaIncumplidaExcepcion(
                        List.of(InfraccionPoliticaContrasenas.EN_HISTORIAL_DE_CONTRASENAS));
            }
            throw new ProveedorIdentidadNoDisponibleExcepcion();
        } catch (RestClientException excepcion) {
            throw new ProveedorIdentidadNoDisponibleExcepcion();
        }
    }

    @Override
    public void cerrarSesiones(String idUsuario, CredencialesProveedorIdentidad credenciales) {
        String token = obtenerTokenAdministrativo(credenciales);
        try {
            cliente.post()
                    .uri("/admin/realms/{reino}/users/{idUsuario}/logout", reino, idUsuario)
                    .headers(cabeceras -> cabeceras.setBearerAuth(token))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException excepcion) {
            throw new ProveedorIdentidadNoDisponibleExcepcion();
        }
    }

    private String obtenerTokenAdministrativo(CredencialesProveedorIdentidad credenciales) {
        TokenAdministrativo token = tokenEnCache;
        if (token != null && token.vigenteEn(reloj.instant())) {
            return token.valor();
        }
        synchronized (this) {
            token = tokenEnCache;
            if (token != null && token.vigenteEn(reloj.instant())) {
                return token.valor();
            }
            tokenEnCache = solicitarTokenAdministrativo(credenciales);
            return tokenEnCache.valor();
        }
    }

    private TokenAdministrativo solicitarTokenAdministrativo(CredencialesProveedorIdentidad credenciales) {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("grant_type", "client_credentials");
        formulario.add("client_id", clienteAdministrativo);
        formulario.add("client_secret", credenciales.secretoClienteAdministrativo());
        try {
            Map<?, ?> respuesta = cliente.post()
                    .uri("/realms/{reino}/protocol/openid-connect/token", reino)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .body(Map.class);
            if (respuesta == null || !(respuesta.get("access_token") instanceof String valor) || valor.isBlank()) {
                throw new ProveedorIdentidadNoDisponibleExcepcion();
            }
            long vigencia = respuesta.get("expires_in") instanceof Number numero
                    ? numero.longValue() : VIGENCIA_PREDETERMINADA_SEGUNDOS;
            Instant expiracion = reloj.instant().plusSeconds(Math.max(1, vigencia - MARGEN_RENOVACION_SEGUNDOS));
            return new TokenAdministrativo(valor, expiracion);
        } catch (RestClientException excepcion) {
            throw new ProveedorIdentidadNoDisponibleExcepcion();
        }
    }

    private record TokenAdministrativo(String valor, Instant expiracion) {

        private boolean vigenteEn(Instant instante) {
            return expiracion.isAfter(instante);
        }

        @Override
        public String toString() {
            return "TokenAdministrativo[OCULTO]";
        }
    }
}

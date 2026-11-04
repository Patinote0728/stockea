package co.stockea.cuenta.infraestructura.vault;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdaptadorVaultSecretosTest {

    @Test
    void obtieneLasCredencialesDelIdpConLasClavesDelSecretoKv() {
        var credenciales = AdaptadorVaultSecretos.mapearCredencialesIdp(Map.of(
                "pwd-verifier-client-secret", "secreto-verificador",
                "admin-api-client-secret", "secreto-administrador"));

        assertEquals("secreto-verificador", credenciales.secretoClienteVerificador());
        assertEquals("secreto-administrador", credenciales.secretoClienteAdministrativo());
        assertFalse(credenciales.toString().contains("secreto-verificador"));
        assertFalse(credenciales.toString().contains("secreto-administrador"));
    }

    @Test
    void obtieneLasCredencialesSmtpConLasClavesDelSecretoKv() {
        var credenciales = AdaptadorVaultSecretos.mapearCredencialesCorreo(Map.of(
                "smtp-username", "usuario-smtp",
                "smtp-password", "secreto-smtp"));

        assertEquals("usuario-smtp", credenciales.usuario());
        assertEquals("secreto-smtp", credenciales.contrasena());
        assertFalse(credenciales.toString().contains("secreto-smtp"));
    }

    @Test
    void rechazaValoresDeRellenoOClavesAusentes() {
        assertThrows(IllegalStateException.class, () ->
                AdaptadorVaultSecretos.mapearCredencialesIdp(Map.of(
                        "pwd-verifier-client-secret", "CAMBIAR-POR-EL-SECRETO-REAL",
                        "admin-api-client-secret", "secreto-administrador")));
        assertThrows(IllegalStateException.class, () ->
                AdaptadorVaultSecretos.mapearCredencialesCorreo(Map.of("smtp-username", "usuario-smtp")));
    }
}

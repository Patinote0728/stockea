package co.stockea.spike;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.vault.authentication.AppRoleAuthentication;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions.RoleId;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions.SecretId;
import org.springframework.vault.authentication.SimpleSessionManager;
import org.springframework.vault.client.VaultClient;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.Versioned;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * SPK-007: el Backend se autentica ante Vault con AppRole y lee {@code secret/stockea/idp} (KV v2).
 *
 * <ul>
 *   <li>RoleId: propiedad {@code stockea.vault.role-id} (propiedad de sistema o variable de entorno
 *       {@code STOCKEA_VAULT_ROLE_ID}).</li>
 *   <li>SecretId: archivo {@code .vault/secret-id} (ignorado por git).</li>
 * </ul>
 *
 * Nunca se imprimen ni se incluyen en mensajes de aserción los valores del RoleId, SecretId, token o secreto.
 * Si falta la configuración, la prueba se omite en lugar de fallar.
 */
class Spk007VaultAppRoleSpikeTest {

    private static final String VAULT_URI = "http://localhost:8200";
    private static final String ROLE_ID_PROPERTY = "stockea.vault.role-id";
    private static final Path SECRET_ID_FILE = Path.of(".vault", "secret-id");
    private static final String KV_MOUNT = "secret";
    private static final String SECRET_PATH = "stockea/idp";

    @Test
    void se_autentica_con_approle_y_lee_el_secreto_del_idp() throws IOException {
        // StandardEnvironment resuelve propiedades de sistema y variables de entorno (relaxed: STOCKEA_VAULT_ROLE_ID).
        String roleId = new StandardEnvironment().getProperty(ROLE_ID_PROPERTY);
        Assumptions.assumeTrue(roleId != null && !roleId.isBlank(),
                "Falta la propiedad " + ROLE_ID_PROPERTY);
        Assumptions.assumeTrue(Files.isRegularFile(SECRET_ID_FILE),
                "Falta el archivo " + SECRET_ID_FILE);

        String secretId = Files.readString(SECRET_ID_FILE).strip();
        Assumptions.assumeFalse(secretId.isEmpty(), "El archivo " + SECRET_ID_FILE + " está vacío");

        AppRoleAuthenticationOptions options = AppRoleAuthenticationOptions.builder()
                .roleId(RoleId.provided(roleId.strip()))
                .secretId(SecretId.provided(secretId))
                .build();

        VaultClient vaultClient = VaultClient.create(VaultEndpoint.from(VAULT_URI));
        AppRoleAuthentication authentication = new AppRoleAuthentication(options, vaultClient);
        VaultTemplate vault = new VaultTemplate(vaultClient, new SimpleSessionManager(authentication));

        Versioned<Map<String, Object>> secret = vault.opsForVersionedKeyValue(KV_MOUNT).get(SECRET_PATH);

        assertNotNull(secret, "No existe el secreto " + KV_MOUNT + "/" + SECRET_PATH);
        assertNotNull(secret.getData(), "El secreto " + KV_MOUNT + "/" + SECRET_PATH + " no tiene datos");
        assertFalse(secret.getRequiredData().isEmpty(),
                "El secreto " + KV_MOUNT + "/" + SECRET_PATH + " está vacío");
    }
}
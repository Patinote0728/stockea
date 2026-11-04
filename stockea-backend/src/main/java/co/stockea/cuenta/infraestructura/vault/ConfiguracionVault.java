package co.stockea.cuenta.infraestructura.vault;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.vault.authentication.AppRoleAuthentication;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions.RoleId;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions.SecretId;
import org.springframework.vault.authentication.SimpleSessionManager;
import org.springframework.vault.client.VaultClient;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration(proxyBeanMethods = false)
public class ConfiguracionVault {

    @Bean
    @Lazy
    VaultTemplate plantillaVault(@Value("${stockea.vault.uri}") String uri,
                                 @Value("${stockea.vault.role-id}") String roleId,
                                 @Value("${stockea.vault.secret-id-file}") String rutaSecretId) {
        try {
            if (roleId == null || roleId.isBlank()) {
                throw new IllegalStateException("Falta configurar el RoleId de Vault");
            }
            String secretId = Files.readString(Path.of(rutaSecretId)).strip();
            if (secretId.isBlank()) {
                throw new IllegalStateException("El archivo de SecretId de Vault está vacío");
            }

            VaultClient cliente = VaultClient.create(VaultEndpoint.from(uri));
            AppRoleAuthenticationOptions opciones = AppRoleAuthenticationOptions.builder()
                    .roleId(RoleId.provided(roleId.strip()))
                    .secretId(SecretId.provided(secretId))
                    .build();
            AppRoleAuthentication autenticacion = new AppRoleAuthentication(opciones, cliente);
            return new VaultTemplate(cliente, new SimpleSessionManager(autenticacion));
        } catch (IOException | RuntimeException excepcion) {
            throw new IllegalStateException("No se pudo preparar el acceso a Vault", excepcion);
        }
    }
}

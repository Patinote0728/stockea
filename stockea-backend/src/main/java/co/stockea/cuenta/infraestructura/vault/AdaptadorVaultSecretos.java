package co.stockea.cuenta.infraestructura.vault;

import co.stockea.cuenta.aplicacion.modelo.CredencialesCorreo;
import co.stockea.cuenta.aplicacion.modelo.CredencialesProveedorIdentidad;
import co.stockea.cuenta.aplicacion.puerto.PuertoSecretos;
import co.stockea.cuenta.dominio.excepcion.ProveedorIdentidadNoDisponibleExcepcion;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.Versioned;

import java.util.Map;

@Component
public final class AdaptadorVaultSecretos implements PuertoSecretos {

    private static final String MONTAJE_KV = "secret";
    private static final String RUTA_IDP = "stockea/idp";
    private static final String RUTA_CORREO = "stockea/mail";
    private static final String VALOR_RELLENO = "CAMBIAR-POR-EL-SECRETO-REAL";

    private final VaultTemplate vault;

    public AdaptadorVaultSecretos(@Lazy VaultTemplate vault) {
        this.vault = vault;
    }

    @Override
    public CredencialesProveedorIdentidad obtenerCredencialesProveedorIdentidad() {
        try {
            return mapearCredencialesIdp(leer(RUTA_IDP));
        } catch (RuntimeException excepcion) {
            throw new ProveedorIdentidadNoDisponibleExcepcion();
        }
    }

    @Override
    public CredencialesCorreo obtenerCredencialesCorreo() {
        try {
            return mapearCredencialesCorreo(leer(RUTA_CORREO));
        } catch (RuntimeException excepcion) {
            throw new ProveedorIdentidadNoDisponibleExcepcion();
        }
    }

    private Map<String, Object> leer(String ruta) {
        Versioned<Map<String, Object>> secreto = vault.opsForVersionedKeyValue(MONTAJE_KV).get(ruta);
        if (secreto == null || secreto.getData() == null || secreto.getData().isEmpty()) {
            throw new IllegalStateException("No se encontró el secreto requerido en Vault");
        }
        return secreto.getRequiredData();
    }

    private static String secreto(Map<String, Object> datos, String clave) {
        Object valor = datos.get(clave);
        if (!(valor instanceof String texto) || texto.isBlank() || VALOR_RELLENO.equals(texto)) {
            throw new IllegalStateException("Falta un secreto requerido en Vault");
        }
        return texto;
    }

    static CredencialesProveedorIdentidad mapearCredencialesIdp(Map<String, Object> datos) {
        return new CredencialesProveedorIdentidad(
                secreto(datos, "pwd-verifier-client-secret"),
                secreto(datos, "admin-api-client-secret"));
    }

    static CredencialesCorreo mapearCredencialesCorreo(Map<String, Object> datos) {
        return new CredencialesCorreo(
                secreto(datos, "smtp-username"),
                secreto(datos, "smtp-password"));
    }
}

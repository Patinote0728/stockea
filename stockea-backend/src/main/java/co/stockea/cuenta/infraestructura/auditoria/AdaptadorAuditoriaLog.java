package co.stockea.cuenta.infraestructura.auditoria;

import co.stockea.cuenta.aplicacion.puerto.PuertoAuditoria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public final class AdaptadorAuditoriaLog implements PuertoAuditoria {

    private static final Logger REGISTRO = LoggerFactory.getLogger(AdaptadorAuditoriaLog.class);

    @Override
    public void registrarIntentoCambioContrasena(String idUsuario, String origen, boolean exitoso) {
        String origenSeguro = origen.replace('\r', '_').replace('\n', '_');
        REGISTRO.info("Intento de cambio de contrasena; instante={}, usuario={}, origen={}, exitoso={}",
                Instant.now(), idUsuario, origenSeguro, exitoso);
    }
}

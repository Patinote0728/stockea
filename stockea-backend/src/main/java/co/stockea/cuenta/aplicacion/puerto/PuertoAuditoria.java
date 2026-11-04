package co.stockea.cuenta.aplicacion.puerto;

public interface PuertoAuditoria {

    /** Registra el instante, el usuario, el origen y el resultado, sin incluir contraseñas ni secretos. */
    void registrarIntentoCambioContrasena(String idUsuario, String origen, boolean exitoso);
}

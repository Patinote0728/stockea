package co.stockea.cuenta.aplicacion.puerto;

public interface PuertoNotificaciones {

    /**
     * Programa el aviso de seguridad sin hacer que la disponibilidad del servidor de correo
     * determine el resultado del cambio. El adaptador debe realizar el envío de forma asíncrona
     * y gestionar sus reintentos.
     */
    void programarAvisoCambioContrasena(String correo);
}

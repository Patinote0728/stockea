package co.stockea.cuenta.aplicacion.puerto;

import co.stockea.cuenta.aplicacion.modelo.CredencialesProveedorIdentidad;
import co.stockea.cuenta.dominio.ContrasenaPlana;

public interface PuertoProveedorIdentidad {

    /**
     * Devuelve falso solo cuando el proveedor rechaza la contraseña actual. Los fallos de
     * comunicación deben convertirse en ProveedorIdentidadNoDisponibleExcepcion.
     */
    boolean verificarContrasenaActual(String nombreUsuario, ContrasenaPlana contrasenaActual,
                                      CredencialesProveedorIdentidad credenciales);

    /**
     * Actualiza la contraseña en el proveedor. La implementación debe traducir un rechazo de la
     * política del proveedor a PoliticaContrasenaIncumplidaExcepcion y un fallo de disponibilidad
     * a ProveedorIdentidadNoDisponibleExcepcion.
     */
    void actualizarContrasena(String idUsuario, ContrasenaPlana contrasenaNueva,
                              CredencialesProveedorIdentidad credenciales);

    /** Los fallos de comunicación deben convertirse en ProveedorIdentidadNoDisponibleExcepcion. */
    void cerrarSesiones(String idUsuario, CredencialesProveedorIdentidad credenciales);
}

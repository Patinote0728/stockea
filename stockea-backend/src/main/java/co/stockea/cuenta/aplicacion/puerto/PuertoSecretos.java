package co.stockea.cuenta.aplicacion.puerto;

import co.stockea.cuenta.aplicacion.modelo.CredencialesProveedorIdentidad;
import co.stockea.cuenta.aplicacion.modelo.CredencialesCorreo;

public interface PuertoSecretos {

    /** Lee del almacén seguro las credenciales vigentes; nunca debe registrarlas en logs. */
    CredencialesProveedorIdentidad obtenerCredencialesProveedorIdentidad();

    /** Lee credenciales SMTP solo cuando el adaptador de correo requiere autenticación. */
    CredencialesCorreo obtenerCredencialesCorreo();
}

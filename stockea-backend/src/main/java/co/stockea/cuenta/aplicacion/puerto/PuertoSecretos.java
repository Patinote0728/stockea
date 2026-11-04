package co.stockea.cuenta.aplicacion.puerto;

import co.stockea.cuenta.aplicacion.modelo.CredencialesProveedorIdentidad;

public interface PuertoSecretos {

    /** Lee del almacén seguro las credenciales vigentes; nunca debe registrarlas en logs. */
    CredencialesProveedorIdentidad obtenerCredencialesProveedorIdentidad();
}

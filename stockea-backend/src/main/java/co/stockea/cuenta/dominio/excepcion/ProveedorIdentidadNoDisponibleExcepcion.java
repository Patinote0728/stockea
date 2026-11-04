package co.stockea.cuenta.dominio.excepcion;

public final class ProveedorIdentidadNoDisponibleExcepcion extends RuntimeException {

    public ProveedorIdentidadNoDisponibleExcepcion() {
        super("El proveedor de identidad o de secretos no está disponible");
    }
}

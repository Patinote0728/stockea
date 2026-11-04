package co.stockea.cuenta.dominio.excepcion;

public final class ContrasenaActualInvalidaExcepcion extends RuntimeException {

    public ContrasenaActualInvalidaExcepcion() {
        super("La contraseña actual no es válida");
    }
}

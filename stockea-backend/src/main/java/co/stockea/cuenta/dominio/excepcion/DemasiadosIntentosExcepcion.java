package co.stockea.cuenta.dominio.excepcion;

public final class DemasiadosIntentosExcepcion extends RuntimeException {

    public DemasiadosIntentosExcepcion() {
        super("Se superó el límite de intentos permitidos");
    }
}

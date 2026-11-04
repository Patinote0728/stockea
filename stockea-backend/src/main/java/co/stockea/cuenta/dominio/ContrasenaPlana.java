package co.stockea.cuenta.dominio;

public final class ContrasenaPlana {

    private final String valor;

    public ContrasenaPlana(String valor) {
        if (valor == null || valor.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede ser nula ni vacía");
        }
        this.valor = valor;
    }

    /** Expone el valor solo para enviarlo al proveedor de identidad; nunca debe registrarse ni persistirse. */
    public String valor() {
        return valor;
    }

    @Override
    public String toString() {
        return "ContrasenaPlana[OCULTA]";
    }
}

package co.stockea.cuenta.aplicacion.modelo;

public final class CredencialesProveedorIdentidad {

    private final String secretoClienteVerificador;
    private final String secretoClienteAdministrativo;

    public CredencialesProveedorIdentidad(String secretoClienteVerificador, String secretoClienteAdministrativo) {
        if (secretoClienteVerificador == null || secretoClienteVerificador.isBlank()) {
            throw new IllegalArgumentException("El secreto del cliente verificador no puede ser nulo ni vacío");
        }
        if (secretoClienteAdministrativo == null || secretoClienteAdministrativo.isBlank()) {
            throw new IllegalArgumentException("El secreto del cliente administrativo no puede ser nulo ni vacío");
        }
        this.secretoClienteVerificador = secretoClienteVerificador;
        this.secretoClienteAdministrativo = secretoClienteAdministrativo;
    }

    public String secretoClienteVerificador() {
        return secretoClienteVerificador;
    }

    public String secretoClienteAdministrativo() {
        return secretoClienteAdministrativo;
    }

    @Override
    public String toString() {
        return "CredencialesProveedorIdentidad[OCULTAS]";
    }
}

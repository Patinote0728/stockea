package co.stockea.cuenta.aplicacion.modelo;

public final class CredencialesCorreo {

    private final String usuario;
    private final String contrasena;

    public CredencialesCorreo(String usuario, String contrasena) {
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El usuario SMTP no puede ser nulo ni vacío");
        }
        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("La contraseña SMTP no puede ser nula ni vacía");
        }
        this.usuario = usuario;
        this.contrasena = contrasena;
    }

    public String usuario() {
        return usuario;
    }

    public String contrasena() {
        return contrasena;
    }

    @Override
    public String toString() {
        return "CredencialesCorreo[OCULTAS]";
    }
}

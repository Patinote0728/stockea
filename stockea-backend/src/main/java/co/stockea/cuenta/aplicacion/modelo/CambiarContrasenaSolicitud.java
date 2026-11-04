package co.stockea.cuenta.aplicacion.modelo;

import co.stockea.cuenta.dominio.ContrasenaPlana;

public final class CambiarContrasenaSolicitud {

    private final String idUsuario;
    private final String tipoDocumento;
    private final String numeroDocumento;
    private final String correo;
    private final String origenSolicitud;
    private final ContrasenaPlana contrasenaActual;
    private final ContrasenaPlana contrasenaNueva;

    public CambiarContrasenaSolicitud(String idUsuario, String tipoDocumento, String numeroDocumento, String correo,
                                      String origenSolicitud, ContrasenaPlana contrasenaActual,
                                      ContrasenaPlana contrasenaNueva) {
        this.idUsuario = validarTexto(idUsuario, "El identificador de usuario");
        this.tipoDocumento = validarTexto(tipoDocumento, "El tipo de documento");
        this.numeroDocumento = validarTexto(numeroDocumento, "El numero de documento");
        this.correo = validarTexto(correo, "El correo");
        this.origenSolicitud = validarTexto(origenSolicitud, "El origen de la solicitud");
        if (contrasenaActual == null) {
            throw new IllegalArgumentException("La contraseña actual no puede ser nula");
        }
        if (contrasenaNueva == null) {
            throw new IllegalArgumentException("La contraseña nueva no puede ser nula");
        }
        this.contrasenaActual = contrasenaActual;
        this.contrasenaNueva = contrasenaNueva;
    }

    public String idUsuario() {
        return idUsuario;
    }

    public String tipoDocumento() {
        return tipoDocumento;
    }

    public String numeroDocumento() {
        return numeroDocumento;
    }

    public String correo() {
        return correo;
    }

    public String origenSolicitud() {
        return origenSolicitud;
    }

    public ContrasenaPlana contrasenaActual() {
        return contrasenaActual;
    }

    public ContrasenaPlana contrasenaNueva() {
        return contrasenaNueva;
    }

    public String nombreUsuarioProveedorIdentidad() {
        return tipoDocumento + "-" + numeroDocumento;
    }

    @Override
    public String toString() {
        return "CambiarContrasenaSolicitud[datos-sensibles-OCULTOS]";
    }

    private static String validarTexto(String valor, String nombreCampo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(nombreCampo + " no puede ser nulo ni vacío");
        }
        return valor;
    }
}

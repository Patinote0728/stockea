package co.stockea.cuenta.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudCambioContrasenaHttp(
        @NotBlank @Size(max = 1024) String contrasenaActual,
        @NotBlank @Size(max = 1024) String contrasenaNueva) {

    @Override
    public String toString() {
        return "SolicitudCambioContrasenaHttp[datos-sensibles-OCULTOS]";
    }
}

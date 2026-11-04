package co.stockea.cuenta.aplicacion;

import co.stockea.cuenta.aplicacion.modelo.CambiarContrasenaSolicitud;
import co.stockea.cuenta.aplicacion.modelo.CredencialesProveedorIdentidad;
import co.stockea.cuenta.aplicacion.puerto.PuertoAuditoria;
import co.stockea.cuenta.aplicacion.puerto.PuertoNotificaciones;
import co.stockea.cuenta.aplicacion.puerto.PuertoProveedorIdentidad;
import co.stockea.cuenta.aplicacion.puerto.PuertoSecretos;
import co.stockea.cuenta.dominio.PoliticaContrasena;
import co.stockea.cuenta.dominio.ResultadoHistorialContrasena;
import co.stockea.cuenta.dominio.excepcion.ContrasenaActualInvalidaExcepcion;
import co.stockea.cuenta.dominio.excepcion.PoliticaContrasenaIncumplidaExcepcion;

import java.util.Objects;

public final class CambiarContrasenaCasoDeUso {

    private final PoliticaContrasena politicaContrasena;
    private final PuertoProveedorIdentidad proveedorIdentidad;
    private final PuertoNotificaciones notificaciones;
    private final PuertoSecretos secretos;
    private final PuertoAuditoria auditoria;

    public CambiarContrasenaCasoDeUso(PoliticaContrasena politicaContrasena,
                                     PuertoProveedorIdentidad proveedorIdentidad,
                                     PuertoNotificaciones notificaciones,
                                     PuertoSecretos secretos,
                                     PuertoAuditoria auditoria) {
        this.politicaContrasena = Objects.requireNonNull(politicaContrasena);
        this.proveedorIdentidad = Objects.requireNonNull(proveedorIdentidad);
        this.notificaciones = Objects.requireNonNull(notificaciones);
        this.secretos = Objects.requireNonNull(secretos);
        this.auditoria = Objects.requireNonNull(auditoria);
    }

    public void ejecutar(CambiarContrasenaSolicitud solicitud) {
        Objects.requireNonNull(solicitud, "La solicitud de cambio de contraseña no puede ser nula");
        boolean exitoso = false;
        try {
            CredencialesProveedorIdentidad credenciales = secretos.obtenerCredencialesProveedorIdentidad();
            boolean contrasenaActualValida = proveedorIdentidad.verificarContrasenaActual(
                    solicitud.nombreUsuarioProveedorIdentidad(), solicitud.contrasenaActual(), credenciales);
            if (!contrasenaActualValida) {
                throw new ContrasenaActualInvalidaExcepcion();
            }

            var infracciones = politicaContrasena.validarConHistorial(
                    solicitud.contrasenaNueva(), solicitud.contrasenaActual(),
                    ResultadoHistorialContrasena.NO_REUTILIZADA,
                    solicitud.numeroDocumento(), solicitud.correo());
            if (!infracciones.isEmpty()) {
                throw new PoliticaContrasenaIncumplidaExcepcion(infracciones);
            }

            proveedorIdentidad.actualizarContrasena(solicitud.idUsuario(), solicitud.contrasenaNueva(), credenciales);
            proveedorIdentidad.cerrarSesiones(solicitud.idUsuario(), credenciales);
            notificaciones.programarAvisoCambioContrasena(solicitud.correo());
            exitoso = true;
        } finally {
            auditoria.registrarIntentoCambioContrasena(
                    solicitud.idUsuario(), solicitud.origenSolicitud(), exitoso);
        }
    }
}

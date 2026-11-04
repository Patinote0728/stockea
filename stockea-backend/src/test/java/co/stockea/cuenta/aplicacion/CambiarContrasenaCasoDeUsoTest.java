package co.stockea.cuenta.aplicacion;

import co.stockea.cuenta.aplicacion.modelo.CambiarContrasenaSolicitud;
import co.stockea.cuenta.aplicacion.modelo.CredencialesProveedorIdentidad;
import co.stockea.cuenta.aplicacion.puerto.PuertoAuditoria;
import co.stockea.cuenta.aplicacion.puerto.PuertoNotificaciones;
import co.stockea.cuenta.aplicacion.puerto.PuertoProveedorIdentidad;
import co.stockea.cuenta.aplicacion.puerto.PuertoSecretos;
import co.stockea.cuenta.dominio.ContrasenaPlana;
import co.stockea.cuenta.dominio.InfraccionPoliticaContrasenas;
import co.stockea.cuenta.dominio.PoliticaContrasena;
import co.stockea.cuenta.dominio.excepcion.ContrasenaActualInvalidaExcepcion;
import co.stockea.cuenta.dominio.excepcion.PoliticaContrasenaIncumplidaExcepcion;
import co.stockea.cuenta.dominio.excepcion.ProveedorIdentidadNoDisponibleExcepcion;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CambiarContrasenaCasoDeUsoTest {

    private static final CredencialesProveedorIdentidad CREDENCIALES =
            new CredencialesProveedorIdentidad("secreto-verificador", "secreto-administrador");

    @Test
    void verificaActualizaRevocaNotificaYAuditaEnElOrdenDefinido() {
        var eventos = new ArrayList<String>();
        var proveedorIdentidad = new ProveedorIdentidadDoble(eventos, true);
        var casoDeUso = crearCasoDeUso(eventos, proveedorIdentidad, crearPuertoSecretos(eventos));

        casoDeUso.ejecutar(solicitudValida());

        assertEquals(List.of(
                "secretos",
                "verificar:CC-991122",
                "actualizar:usuario-123",
                "cerrar-sesiones:usuario-123",
                "notificar:persona@example.com",
                "auditar:usuario-123:true:192.0.2.10"), eventos);
    }

    @Test
    void rechazaLaContrasenaActualInvalidaSinCambiarLaCredencial() {
        var eventos = new ArrayList<String>();
        var proveedorIdentidad = new ProveedorIdentidadDoble(eventos, false);
        var casoDeUso = crearCasoDeUso(eventos, proveedorIdentidad, crearPuertoSecretos(eventos));

        var excepcion = assertThrows(ContrasenaActualInvalidaExcepcion.class,
                () -> casoDeUso.ejecutar(solicitudValida()));

        assertFalse(excepcion.getMessage().contains("contrasena-actual-secreta"));
        assertEquals(List.of(
                "secretos",
                "verificar:CC-991122",
                "auditar:usuario-123:false:192.0.2.10"), eventos);
    }

    @Test
    void validaLaPoliticaDespuesDeVerificarLaContrasenaActual() {
        var eventos = new ArrayList<String>();
        var proveedorIdentidad = new ProveedorIdentidadDoble(eventos, true);
        var casoDeUso = crearCasoDeUso(eventos, proveedorIdentidad, crearPuertoSecretos(eventos));
        var solicitud = new CambiarContrasenaSolicitud(
                "usuario-123", "CC", "991122", "persona@example.com", "192.0.2.10",
                new ContrasenaPlana("contrasena-actual-secreta"), new ContrasenaPlana("corta"));

        var excepcion = assertThrows(PoliticaContrasenaIncumplidaExcepcion.class,
                () -> casoDeUso.ejecutar(solicitud));

        assertEquals(List.of(InfraccionPoliticaContrasenas.DEMASIADO_CORTA), excepcion.violaciones());
        assertEquals(List.of(
                "secretos",
                "verificar:CC-991122",
                "auditar:usuario-123:false:192.0.2.10"), eventos);
    }

    @Test
    void registraElIntentoSiKeyVaultNoEstaDisponible() {
        var eventos = new ArrayList<String>();
        var proveedorIdentidad = new ProveedorIdentidadDoble(eventos, true);
        var casoDeUso = crearCasoDeUso(eventos, proveedorIdentidad, () -> {
            eventos.add("secretos");
            throw new ProveedorIdentidadNoDisponibleExcepcion();
        });

        assertThrows(ProveedorIdentidadNoDisponibleExcepcion.class,
                () -> casoDeUso.ejecutar(solicitudValida()));

        assertEquals(List.of("secretos", "auditar:usuario-123:false:192.0.2.10"), eventos);
    }

    @Test
    void noExponeContrasenasAlConvertirLaSolicitudATexto() {
        var solicitud = solicitudValida();

        assertFalse(solicitud.toString().contains("contrasena-actual-secreta"));
        assertFalse(solicitud.toString().contains("contrasena-nueva-secreta"));
    }

    private CambiarContrasenaSolicitud solicitudValida() {
        return new CambiarContrasenaSolicitud(
                "usuario-123", "CC", "991122", "persona@example.com", "192.0.2.10",
                new ContrasenaPlana("contrasena-actual-secreta"), new ContrasenaPlana("contrasena-nueva-secreta"));
    }

    private CambiarContrasenaCasoDeUso crearCasoDeUso(List<String> eventos,
                                                       PuertoProveedorIdentidad proveedorIdentidad,
                                                       PuertoSecretos puertoSecretos) {
        PuertoNotificaciones notificaciones = correo -> eventos.add("notificar:" + correo);
        PuertoAuditoria auditoria = (idUsuario, origen, exitoso) ->
                eventos.add("auditar:" + idUsuario + ":" + exitoso + ":" + origen);
        return new CambiarContrasenaCasoDeUso(
                new PoliticaContrasena(), proveedorIdentidad, notificaciones, puertoSecretos, auditoria);
    }

    private PuertoSecretos crearPuertoSecretos(List<String> eventos) {
        return () -> {
            eventos.add("secretos");
            return CREDENCIALES;
        };
    }

    private static final class ProveedorIdentidadDoble implements PuertoProveedorIdentidad {

        private final List<String> eventos;
        private final boolean contrasenaActualValida;

        private ProveedorIdentidadDoble(List<String> eventos, boolean contrasenaActualValida) {
            this.eventos = eventos;
            this.contrasenaActualValida = contrasenaActualValida;
        }

        @Override
        public boolean verificarContrasenaActual(String nombreUsuario, ContrasenaPlana contrasenaActual,
                                                 CredencialesProveedorIdentidad credenciales) {
            eventos.add("verificar:" + nombreUsuario);
            return contrasenaActualValida;
        }

        @Override
        public void actualizarContrasena(String idUsuario, ContrasenaPlana contrasenaNueva,
                                         CredencialesProveedorIdentidad credenciales) {
            eventos.add("actualizar:" + idUsuario);
        }

        @Override
        public void cerrarSesiones(String idUsuario, CredencialesProveedorIdentidad credenciales) {
            eventos.add("cerrar-sesiones:" + idUsuario);
        }
    }
}

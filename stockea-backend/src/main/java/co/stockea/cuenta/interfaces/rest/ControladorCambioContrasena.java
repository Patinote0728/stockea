package co.stockea.cuenta.interfaces.rest;

import co.stockea.cuenta.aplicacion.CambiarContrasenaCasoDeUso;
import co.stockea.cuenta.aplicacion.modelo.CambiarContrasenaSolicitud;
import co.stockea.cuenta.aplicacion.puerto.PuertoAuditoria;
import co.stockea.cuenta.dominio.ContrasenaPlana;
import co.stockea.cuenta.dominio.excepcion.ContrasenaActualInvalidaExcepcion;
import co.stockea.cuenta.interfaces.seguridad.LimitadorIntentosCambioContrasena;
import co.stockea.cuenta.interfaces.seguridad.LimitadorIntentosCambioContrasena.LimiteIntentosAlcanzadoException;
import co.stockea.cuenta.interfaces.seguridad.LimitadorIntentosCambioContrasena.ReservaIntento;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cuenta")
public final class ControladorCambioContrasena {

    public static final String RUTA = "/api/v1/cuenta/contrasena";

    private final CambiarContrasenaCasoDeUso casoDeUso;
    private final LimitadorIntentosCambioContrasena limitador;
    private final PuertoAuditoria auditoria;

    public ControladorCambioContrasena(CambiarContrasenaCasoDeUso casoDeUso,
                                      LimitadorIntentosCambioContrasena limitador,
                                      PuertoAuditoria auditoria) {
        this.casoDeUso = casoDeUso;
        this.limitador = limitador;
        this.auditoria = auditoria;
    }

    @PostMapping("/contrasena")
    public ResponseEntity<Void> cambiar(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody SolicitudCambioContrasenaHttp cuerpo,
                                        HttpServletRequest peticion) {
        IdentidadAutenticada identidad = IdentidadAutenticada.desde(jwt);
        String origen = peticion.getRemoteAddr();
        if (origen == null || origen.isBlank()) {
            origen = "desconocido";
        }

        ReservaIntento reserva;
        try {
            reserva = limitador.reservar(identidad.idUsuario());
        } catch (LimiteIntentosAlcanzadoException excepcion) {
            auditoria.registrarIntentoCambioContrasena(identidad.idUsuario(), origen, false);
            throw excepcion;
        }

        try (reserva) {
            try {
                casoDeUso.ejecutar(new CambiarContrasenaSolicitud(
                        identidad.idUsuario(), identidad.tipoDocumento(), identidad.numeroDocumento(),
                        identidad.correo(), origen,
                        new ContrasenaPlana(cuerpo.contrasenaActual()),
                        new ContrasenaPlana(cuerpo.contrasenaNueva())));
            } catch (ContrasenaActualInvalidaExcepcion excepcion) {
                reserva.registrarFallo();
                throw excepcion;
            }
        }

        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .build();
    }

    private record IdentidadAutenticada(String idUsuario, String tipoDocumento,
                                        String numeroDocumento, String correo) {

        private static IdentidadAutenticada desde(Jwt jwt) {
            if (jwt == null) {
                throw new DatosIdentidadTokenInvalidosException();
            }
            String idUsuario = jwt.getSubject();
            String usuario = jwt.getClaimAsString("preferred_username");
            String correo = jwt.getClaimAsString("email");
            if (idUsuario == null || idUsuario.isBlank() || usuario == null || correo == null || correo.isBlank()) {
                throw new DatosIdentidadTokenInvalidosException();
            }
            int separador = usuario.indexOf('-');
            if (separador < 1 || separador == usuario.length() - 1) {
                throw new DatosIdentidadTokenInvalidosException();
            }
            return new IdentidadAutenticada(idUsuario, usuario.substring(0, separador),
                    usuario.substring(separador + 1), correo);
        }
    }

    public static final class DatosIdentidadTokenInvalidosException extends RuntimeException {
        public DatosIdentidadTokenInvalidosException() {
            super("El token autenticado no contiene las afirmaciones requeridas");
        }
    }
}

package co.stockea.cuenta.interfaces.rest;

import co.stockea.cuenta.dominio.excepcion.ContrasenaActualInvalidaExcepcion;
import co.stockea.cuenta.dominio.excepcion.PoliticaContrasenaIncumplidaExcepcion;
import co.stockea.cuenta.dominio.excepcion.ProveedorIdentidadNoDisponibleExcepcion;
import co.stockea.cuenta.interfaces.rest.ControladorCambioContrasena.DatosIdentidadTokenInvalidosException;
import co.stockea.cuenta.interfaces.seguridad.LimitadorIntentosCambioContrasena.LimiteIntentosAlcanzadoException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErroresCambioContrasena {

    @ExceptionHandler(ContrasenaActualInvalidaExcepcion.class)
    ResponseEntity<RespuestaErrorHttp> contrasenaActualInvalida() {
        return respuesta(HttpStatus.BAD_REQUEST, "CREDENCIALES_INVALIDAS",
                "No fue posible cambiar la contraseña. Verifica los datos ingresados.");
    }

    @ExceptionHandler(PoliticaContrasenaIncumplidaExcepcion.class)
    ResponseEntity<RespuestaErrorHttp> politicaIncumplida() {
        return respuesta(HttpStatus.BAD_REQUEST, "POLITICA_CONTRASENA",
                "La nueva contraseña no cumple la política requerida.");
    }

    @ExceptionHandler(LimiteIntentosAlcanzadoException.class)
    ResponseEntity<RespuestaErrorHttp> demasiadosIntentos(LimiteIntentosAlcanzadoException excepcion) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.RETRY_AFTER, Long.toString(excepcion.segundosParaReintentar()))
                .body(new RespuestaErrorHttp("LIMITE_INTENTOS",
                        "Superaste el límite de intentos. Intenta nuevamente cuando termine el tiempo de espera."));
    }

    @ExceptionHandler(ProveedorIdentidadNoDisponibleExcepcion.class)
    ResponseEntity<RespuestaErrorHttp> proveedorNoDisponible() {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE",
                "No es posible procesar la solicitud en este momento.");
    }

    @ExceptionHandler(DatosIdentidadTokenInvalidosException.class)
    ResponseEntity<RespuestaErrorHttp> identidadTokenInvalida() {
        return respuesta(HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO",
                "No fue posible validar la identidad de la solicitud.");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<RespuestaErrorHttp> solicitudInvalida() {
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA",
                "La solicitud no tiene un formato válido.");
    }

    private ResponseEntity<RespuestaErrorHttp> respuesta(HttpStatus estado, String codigo, String mensaje) {
        return ResponseEntity.status(estado)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(new RespuestaErrorHttp(codigo, mensaje));
    }
}

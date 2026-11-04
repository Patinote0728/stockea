package co.stockea.cuenta.interfaces.seguridad;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LimitadorIntentosCambioContrasenaTest {

    @Test
    void rechazaElSextoFalloYExpiraElContadorAlCumplirseQuinceMinutos() {
        RelojMutable reloj = new RelojMutable(Instant.parse("2026-11-04T15:00:00Z"));
        var limitador = new LimitadorIntentosCambioContrasena(reloj);

        registrarFallos(limitador, "usuario-1", 5);
        var excepcion = assertThrows(LimitadorIntentosCambioContrasena.LimiteIntentosAlcanzadoException.class,
                () -> limitador.reservar("usuario-1"));
        assertEquals(900, excepcion.segundosParaReintentar());

        reloj.avanzar(Duration.ofMinutes(15));
        assertDoesNotThrow(() -> limitador.reservar("usuario-1").close());
    }

    @Test
    void limitaPorUsuarioYLiberaLosIntentosQueNoFallaron() {
        var limitador = new LimitadorIntentosCambioContrasena(
                new RelojMutable(Instant.parse("2026-11-04T15:00:00Z")));

        for (int intento = 0; intento < 8; intento++) {
            limitador.reservar("usuario-1").close();
        }
        registrarFallos(limitador, "usuario-1", 5);

        assertThrows(LimitadorIntentosCambioContrasena.LimiteIntentosAlcanzadoException.class,
                () -> limitador.reservar("usuario-1"));
        assertDoesNotThrow(() -> limitador.reservar("usuario-2").close());
    }

    @Test
    void reservaSoloCincoSolicitudesConcurrentesMientrasSeDeterminaElResultado() {
        var limitador = new LimitadorIntentosCambioContrasena(
                new RelojMutable(Instant.parse("2026-11-04T15:00:00Z")));
        List<LimitadorIntentosCambioContrasena.ReservaIntento> reservas = new ArrayList<>();
        for (int intento = 0; intento < 5; intento++) {
            reservas.add(limitador.reservar("usuario-1"));
        }

        assertThrows(LimitadorIntentosCambioContrasena.LimiteIntentosAlcanzadoException.class,
                () -> limitador.reservar("usuario-1"));
        reservas.forEach(LimitadorIntentosCambioContrasena.ReservaIntento::close);
        assertDoesNotThrow(() -> limitador.reservar("usuario-1").close());
    }

    private void registrarFallos(LimitadorIntentosCambioContrasena limitador, String idUsuario, int cantidad) {
        for (int intento = 0; intento < cantidad; intento++) {
            limitador.reservar(idUsuario).registrarFallo();
        }
    }

    private static final class RelojMutable extends Clock {
        private Instant instante;

        private RelojMutable(Instant instante) {
            this.instante = instante;
        }

        private void avanzar(Duration duracion) {
            instante = instante.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}

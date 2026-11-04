package co.stockea.cuenta.interfaces.seguridad;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public final class LimitadorIntentosCambioContrasena {

    static final int MAXIMO_INTENTOS = 5;
    static final Duration DURACION_VENTANA = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, VentanaIntentos> ventanas = new ConcurrentHashMap<>();
    private final Clock reloj;

    public LimitadorIntentosCambioContrasena() {
        this(Clock.systemUTC());
    }

    LimitadorIntentosCambioContrasena(Clock reloj) {
        this.reloj = reloj;
    }

    public ReservaIntento reservar(String idUsuario) {
        Instant ahora = reloj.instant();
        VentanaIntentos[] ventanaReservada = new VentanaIntentos[1];
        ventanas.compute(idUsuario, (id, ventanaActual) -> {
            VentanaIntentos ventana = ventanaActual == null ? new VentanaIntentos() : ventanaActual;
            ventana.reiniciarSiVencio(ahora);
            if (ventana.intentosFallidos + ventana.intentosEnCurso >= MAXIMO_INTENTOS) {
                Instant reinicio = ventana.inicioVentana == null
                        ? ahora.plusSeconds(1) : ventana.inicioVentana.plus(DURACION_VENTANA);
                long milisegundos = Duration.between(ahora, reinicio).toMillis();
                long segundos = Math.max(1, (milisegundos + 999) / 1000);
                throw new LimiteIntentosAlcanzadoException(segundos);
            }
            ventana.intentosEnCurso++;
            ventanaReservada[0] = ventana;
            return ventana;
        });
        return new ReservaIntento(idUsuario, ventanaReservada[0]);
    }

    private void finalizar(String idUsuario, VentanaIntentos reservada, boolean fallo) {
        Instant ahora = reloj.instant();
        ventanas.computeIfPresent(idUsuario, (id, ventanaActual) -> {
            if (ventanaActual != reservada) {
                return ventanaActual;
            }
            ventanaActual.reiniciarSiVencio(ahora);
            ventanaActual.intentosEnCurso--;
            if (fallo) {
                if (ventanaActual.inicioVentana == null) {
                    ventanaActual.inicioVentana = ahora;
                }
                ventanaActual.intentosFallidos++;
            }
            return ventanaActual.intentosEnCurso == 0 && ventanaActual.intentosFallidos == 0
                    ? null : ventanaActual;
        });
    }

    private static final class VentanaIntentos {
        private Instant inicioVentana;
        private int intentosFallidos;
        private int intentosEnCurso;

        private void reiniciarSiVencio(Instant ahora) {
            if (inicioVentana != null && !ahora.isBefore(inicioVentana.plus(DURACION_VENTANA))) {
                inicioVentana = null;
                intentosFallidos = 0;
            }
        }
    }

    public final class ReservaIntento implements AutoCloseable {
        private final String idUsuario;
        private final VentanaIntentos ventana;
        private final AtomicBoolean finalizada = new AtomicBoolean();

        private ReservaIntento(String idUsuario, VentanaIntentos ventana) {
            this.idUsuario = idUsuario;
            this.ventana = ventana;
        }

        public void registrarFallo() {
            if (finalizada.compareAndSet(false, true)) {
                finalizar(idUsuario, ventana, true);
            }
        }

        @Override
        public void close() {
            if (finalizada.compareAndSet(false, true)) {
                finalizar(idUsuario, ventana, false);
            }
        }
    }

    public static final class LimiteIntentosAlcanzadoException extends RuntimeException {
        private final long segundosParaReintentar;

        private LimiteIntentosAlcanzadoException(long segundosParaReintentar) {
            super("Se superó el límite de intentos permitidos");
            this.segundosParaReintentar = segundosParaReintentar;
        }

        public long segundosParaReintentar() {
            return segundosParaReintentar;
        }
    }
}

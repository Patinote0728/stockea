package co.stockea.cuenta.infraestructura.correo;

import co.stockea.cuenta.aplicacion.puerto.PuertoSecretos;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdaptadorNotificacionesSmtpTest {

    @Test
    void enviaElAvisoDeFormaAsincronaSinIncluirDatosSecretos() {
        Queue<Runnable> tareas = new ArrayDeque<>();
        TaskScheduler planificador = crearPlanificador(tareas);
        JavaMailSender remitenteCorreo = mock(JavaMailSender.class);
        var adaptador = crearAdaptador(planificador, () -> remitenteCorreo);

        adaptador.programarAvisoCambioContrasena("persona@example.com");
        assertEquals(1, tareas.size());
        tareas.remove().run();

        org.mockito.ArgumentCaptor<SimpleMailMessage> capturador =
                org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(remitenteCorreo).send(capturador.capture());
        SimpleMailMessage mensaje = capturador.getValue();
        assertEquals("persona@example.com", mensaje.getTo()[0]);
        assertEquals("Aviso de seguridad de STOCKEA", mensaje.getSubject());
        assertFalse(mensaje.getText().contains("contrasena-actual-secreta"));
        assertFalse(mensaje.getText().contains("contrasena-nueva-secreta"));
    }

    @Test
    void reintentaCincoVecesConElProveedorDeCorreoCaido() {
        Queue<Runnable> tareas = new ArrayDeque<>();
        TaskScheduler planificador = crearPlanificador(tareas);
        JavaMailSender remitenteCorreo = mock(JavaMailSender.class);
        doThrow(new MailSendException("fallo smtp"))
                .when(remitenteCorreo).send(any(SimpleMailMessage.class));
        var adaptador = crearAdaptador(planificador, () -> remitenteCorreo);

        adaptador.programarAvisoCambioContrasena("persona@example.com");
        while (!tareas.isEmpty()) {
            tareas.remove().run();
        }

        verify(remitenteCorreo, times(6)).send(any(SimpleMailMessage.class));
        verify(planificador, times(6)).schedule(any(Runnable.class), any(Instant.class));
    }

    private TaskScheduler crearPlanificador(Queue<Runnable> tareas) {
        TaskScheduler planificador = mock(TaskScheduler.class);
        when(planificador.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(invocacion -> {
            tareas.add(invocacion.getArgument(0));
            return mock(ScheduledFuture.class);
        });
        return planificador;
    }

    private AdaptadorNotificacionesSmtp crearAdaptador(TaskScheduler planificador,
                                                       java.util.function.Supplier<JavaMailSender> remitente) {
        PuertoSecretos secretos = mock(PuertoSecretos.class);
        return new AdaptadorNotificacionesSmtp(secretos, planificador, "localhost", 1025,
                "no-responder@stockea.local", false, false, Duration.ofSeconds(2), remitente);
    }
}

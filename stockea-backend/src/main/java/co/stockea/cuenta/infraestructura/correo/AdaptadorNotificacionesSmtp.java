package co.stockea.cuenta.infraestructura.correo;

import co.stockea.cuenta.aplicacion.modelo.CredencialesCorreo;
import co.stockea.cuenta.aplicacion.puerto.PuertoNotificaciones;
import co.stockea.cuenta.aplicacion.puerto.PuertoSecretos;
import co.stockea.cuenta.dominio.excepcion.ProveedorIdentidadNoDisponibleExcepcion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.function.Supplier;

@Component
public final class AdaptadorNotificacionesSmtp implements PuertoNotificaciones {

    private static final Logger REGISTRO = LoggerFactory.getLogger(AdaptadorNotificacionesSmtp.class);
    private static final int MAXIMO_REINTENTOS = 5;
    private static final long[] ESPERA_SEGUNDOS = {1, 2, 4, 8, 16};

    private final PuertoSecretos secretos;
    private final TaskScheduler planificador;
    private final String host;
    private final int puerto;
    private final String remitente;
    private final boolean autenticacionHabilitada;
    private final boolean startTlsHabilitado;
    private final Duration timeout;
    private final Supplier<JavaMailSender> proveedorRemitenteCorreo;

    @Autowired
    public AdaptadorNotificacionesSmtp(PuertoSecretos secretos,
                                      @Qualifier("planificadorAvisosCorreo") TaskScheduler planificador,
                                      @Value("${stockea.correo.host}") String host,
                                      @Value("${stockea.correo.puerto}") int puerto,
                                      @Value("${stockea.correo.remitente}") String remitente,
                                      @Value("${stockea.correo.autenticacion-habilitada}") boolean autenticacionHabilitada,
                                      @Value("${stockea.correo.starttls-habilitado}") boolean startTlsHabilitado,
                                      @Value("${stockea.correo.timeout}") Duration timeout) {
        this(secretos, planificador, host, puerto, remitente, autenticacionHabilitada,
                startTlsHabilitado, timeout, null);
    }

    AdaptadorNotificacionesSmtp(PuertoSecretos secretos, TaskScheduler planificador,
                                String host, int puerto, String remitente,
                                boolean autenticacionHabilitada, boolean startTlsHabilitado,
                                Duration timeout, Supplier<JavaMailSender> proveedorRemitenteCorreo) {
        this.secretos = secretos;
        this.planificador = planificador;
        this.host = host;
        this.puerto = puerto;
        this.remitente = remitente;
        this.autenticacionHabilitada = autenticacionHabilitada;
        this.startTlsHabilitado = startTlsHabilitado;
        this.timeout = timeout;
        this.proveedorRemitenteCorreo = proveedorRemitenteCorreo == null
                ? this::crearRemitenteCorreo : proveedorRemitenteCorreo;
    }

    @Override
    public void programarAvisoCambioContrasena(String correo) {
        programar(correo, 0, Instant.now());
    }

    private void programar(String correo, int numeroReintento, Instant instante) {
        try {
            planificador.schedule(() -> enviar(correo, numeroReintento), instante);
        } catch (RuntimeException excepcion) {
            REGISTRO.error("No fue posible programar el envio del aviso de seguridad; reintento {}", numeroReintento);
        }
    }

    private void enviar(String correo, int numeroReintento) {
        try {
            JavaMailSender remitenteCorreo = proveedorRemitenteCorreo.get();
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(correo);
            mensaje.setSubject("Aviso de seguridad de STOCKEA");
            mensaje.setText("Se cambio la contraseña de tu cuenta STOCKEA. "
                    + "Si no fuiste tu, comunicate con el administrador de tu negocio.");
            remitenteCorreo.send(mensaje);
        } catch (MailException | ProveedorIdentidadNoDisponibleExcepcion excepcion) {
            programarSiguienteIntento(correo, numeroReintento);
        } catch (RuntimeException excepcion) {
            programarSiguienteIntento(correo, numeroReintento);
        }
    }

    private JavaMailSender crearRemitenteCorreo() {
        JavaMailSenderImpl remitenteCorreo = new JavaMailSenderImpl();
        remitenteCorreo.setHost(host);
        remitenteCorreo.setPort(puerto);
        Properties propiedades = remitenteCorreo.getJavaMailProperties();
        propiedades.put("mail.smtp.auth", Boolean.toString(autenticacionHabilitada));
        propiedades.put("mail.smtp.starttls.enable", Boolean.toString(startTlsHabilitado));
        propiedades.put("mail.smtp.connectiontimeout", Long.toString(timeout.toMillis()));
        propiedades.put("mail.smtp.timeout", Long.toString(timeout.toMillis()));
        propiedades.put("mail.smtp.writetimeout", Long.toString(timeout.toMillis()));
        if (autenticacionHabilitada) {
            CredencialesCorreo credenciales = secretos.obtenerCredencialesCorreo();
            remitenteCorreo.setUsername(credenciales.usuario());
            remitenteCorreo.setPassword(credenciales.contrasena());
        }
        return remitenteCorreo;
    }

    private void programarSiguienteIntento(String correo, int numeroReintento) {
        if (numeroReintento >= MAXIMO_REINTENTOS) {
            REGISTRO.error("No fue posible enviar el aviso de seguridad tras {} reintentos", MAXIMO_REINTENTOS);
            return;
        }
        long segundos = ESPERA_SEGUNDOS[numeroReintento];
        programar(correo, numeroReintento + 1, Instant.now().plusSeconds(segundos));
    }
}

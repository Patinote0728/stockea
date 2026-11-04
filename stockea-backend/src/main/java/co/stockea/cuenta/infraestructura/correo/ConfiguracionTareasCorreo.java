package co.stockea.cuenta.infraestructura.correo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
public class ConfiguracionTareasCorreo {

    @Bean
    TaskScheduler planificadorAvisosCorreo() {
        ThreadPoolTaskScheduler planificador = new ThreadPoolTaskScheduler();
        planificador.setPoolSize(2);
        planificador.setThreadNamePrefix("reintento-correo-");
        planificador.setWaitForTasksToCompleteOnShutdown(true);
        planificador.setAwaitTerminationSeconds(5);
        return planificador;
    }
}

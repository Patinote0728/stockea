package co.stockea.cuenta.infraestructura.configuracion;

import co.stockea.cuenta.aplicacion.CambiarContrasenaCasoDeUso;
import co.stockea.cuenta.aplicacion.puerto.PuertoAuditoria;
import co.stockea.cuenta.aplicacion.puerto.PuertoNotificaciones;
import co.stockea.cuenta.aplicacion.puerto.PuertoProveedorIdentidad;
import co.stockea.cuenta.aplicacion.puerto.PuertoSecretos;
import co.stockea.cuenta.dominio.PoliticaContrasena;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ConfiguracionCasoDeUso {

    @Bean
    PoliticaContrasena politicaContrasena() {
        return new PoliticaContrasena();
    }

    @Bean
    CambiarContrasenaCasoDeUso cambiarContrasenaCasoDeUso(PoliticaContrasena politicaContrasena,
                                                          PuertoProveedorIdentidad proveedorIdentidad,
                                                          PuertoNotificaciones notificaciones,
                                                          PuertoSecretos secretos,
                                                          PuertoAuditoria auditoria) {
        return new CambiarContrasenaCasoDeUso(politicaContrasena, proveedorIdentidad,
                notificaciones, secretos, auditoria);
    }
}

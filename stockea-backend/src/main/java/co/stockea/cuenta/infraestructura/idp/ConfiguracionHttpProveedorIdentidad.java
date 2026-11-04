package co.stockea.cuenta.infraestructura.idp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class ConfiguracionHttpProveedorIdentidad {

    @Bean
    RestClient clienteProveedorIdentidad(@Value("${stockea.proveedor-identidad.url-base}") String urlBase,
                                         @Value("${stockea.proveedor-identidad.timeout}") Duration timeout) {
        HttpClient clienteJava = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(clienteJava);
        fabrica.setReadTimeout(timeout);
        return RestClient.builder().baseUrl(urlBase).requestFactory(fabrica).build();
    }
}

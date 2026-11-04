package co.stockea.cuenta.interfaces.seguridad;

import co.stockea.cuenta.interfaces.rest.ControladorCambioContrasena;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class ConfiguracionSeguridad {

    @Bean
    JwtDecoder decodificadorJwt(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String uriJwks,
                                @Value("${stockea.seguridad.issuer-uri}") String emisor) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withJwkSetUri(uriJwks).build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(emisor));
        return decodificador;
    }

    @Bean
    SecurityFilterChain filtroSeguridad(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers(HttpMethod.POST, ControladorCambioContrasena.RUTA).authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(recurso -> recurso.jwt(jwt -> { }))
                .build();
    }
}

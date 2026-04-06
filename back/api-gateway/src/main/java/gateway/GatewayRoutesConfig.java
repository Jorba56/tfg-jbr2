package gateway; // Ajusta a tu paquete real

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    // Leemos la URL de la variable de entorno de Railway
    @Value("${URI_USUARIOS:http://localhost:8081}")
    private String uriUsuarios;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("usuarios-service", r -> r.path("/auth/**", "/usuarios/**")
                        .uri(uriUsuarios))
                .build();
    }
}

package gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Value("${URI_USUARIOS:http://localhost:8081}")
    private String uriUsuarios;

    // 1. Añadimos la variable para incidencias
    @Value("${URI_INCIDENCIAS:http://localhost:8082}")
    private String uriIncidencias;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("usuarios-service", r -> r.path("/auth/**", "/usuarios/**")
                        .uri(uriUsuarios))
                // 2. Añadimos la nueva ruta para incidencias
                .route("incidencias-service", r -> r.path("/incidencias/**")
                        .uri(uriIncidencias))
                .build();
    }
}
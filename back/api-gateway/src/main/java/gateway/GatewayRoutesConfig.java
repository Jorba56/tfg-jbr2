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

    @Value("${URI_INCIDENCIAS:http://localhost:8082}")
    private String uriIncidencias;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("usuarios-service", r -> r.path(
                                "/auth/**",
                                "/usuarios/**",
                                "/roles/**",
                                "/usuarios_roles/**",
                                "/ws-game/**"
                        )
                        // Eliminamos los CORS duplicados
                        .filters(f -> f
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_UNIQUE")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_UNIQUE")
                        )
                        .uri(uriUsuarios))
                .route("incidencias-service", r -> r.path("/incidencias/**")
                        // También se lo ponemos a incidencias por si en el futuro pasa lo mismo
                        .filters(f -> f
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_UNIQUE")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_UNIQUE")
                        )
                        .uri(uriIncidencias))
                .build();
    }
}
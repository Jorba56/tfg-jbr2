package gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // LA SOLUCIÓN: Usar Patterns en lugar de Origins estrictos.
        // Esto permite variaciones invisibles que pueda meter el navegador.
        corsConfig.setAllowedOriginPatterns(Arrays.asList(
                "https://tfg-jbr2.onrender.com",
                "https://*.onrender.com", // Comodín por si Render usa subdominios internos
                "http://localhost:*"      // Comodín para cualquier puerto local
        ));

        // Métodos permitidos
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Con AllowedOriginPatterns SÍ podemos usar el comodín "*" para las cabeceras
        corsConfig.setAllowedHeaders(Arrays.asList("*"));

        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
package com.jorge.incidencias.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Clase de configuración principal de Spring Security.
 * Establece las políticas de seguridad del microservicio, definiendo el manejo de sesiones sin estado (stateless),
 * las rutas públicas/privadas, la integración con CORS y la gestión centralizada de excepciones de seguridad.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @SuppressWarnings("squid:S4502") // deshabilitamos csrf porque usamos jwt, no cookies de sesión, y asi sonar no salta
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. ABRIMOS LA PUERTA A SWAGGER
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // 2. EL TÚNEL PARA LOS ERRORES
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/incidencias/**").permitAll()

                        // 3. SOLO ADMIN PUEDE LEER INCIDENCIAS
                        .requestMatchers("/incidencias/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )
                // Metemos nuestro filtro lector de JWT antes del filtro por defecto de Spring
                .addFilterBefore(new JwtFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
package com.jorge.incidencias;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@OpenAPIDefinition(
        info = @Info(title = "API del Sprint 2", version = "1.0", description = "Generador de Exámenes"),
        security = @SecurityRequirement(name = "bearerAuth")
)
// 2. Configuramos cómo es ese esquema de seguridad (Token JWT en la cabecera)
@SecurityScheme(
        name = "bearerAuth",
        description = "Introduce aquí el token JWT generado en el endpoint de Login",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)

@SpringBootApplication
@EntityScan("com.jorge.incidencias.entity")
public class IncidenciasApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidenciasApplication.class, args);
    }

}

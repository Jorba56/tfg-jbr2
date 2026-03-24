package com.jorge.usuarios.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.List;

/**
 * Componente utilitario encargado de la creación, firmado y validación de tokens JWT.
 * Utiliza el algoritmo HMAC256 basándose en una clave secreta inyectada por variables de entorno.
 */
@Component
public class JwtUtil {

    private static final String SECRET_KEY=System.getenv("SECRET_KEY") != null ? System.getenv("SECRET_KEY") : "clave_secreta_por_defecto_para_tests"; //recoger variable de entorno
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET_KEY);

    /**
     * Genera un nuevo token JWT para un usuario tras un inicio de sesión exitoso.
     * El token incluye el correo como "subject", los roles como un "claim" adicional
     * y tiene un tiempo de validez de 24 horas.
     *
     * @param email Correo electrónico del usuario autenticado.
     * @param roles Lista de roles asignados al usuario.
     * @return String que representa el token JWT firmado.
     */
    public String generarToken(String email, List<String> roles) {
        return JWT.create()
                .withSubject(email)// guardamos el correo en el token
                .withClaim("roles", roles)
                .withIssuedAt(new Date()) // fecha de creación
                .withExpiresAt(new Date(System.currentTimeMillis() + 86400000)) // expira en 1 día
                .sign(ALGORITHM);
    }

    /**
     * Valida la firma matemática del token JWT y extrae el correo electrónico (subject)
     * contenido en él. Lanzará una excepción subyacente si el token ha expirado o ha sido manipulado.
     *
     * @param token Token JWT extraído de la petición del cliente.
     * @return El correo electrónico (subject) si el token es válido.
     */
    public String validarTokenYObtenerEmail(String token) {
        return JWT.require(ALGORITHM)
                .build()
                .verify(token)
                .getSubject();
    }
}
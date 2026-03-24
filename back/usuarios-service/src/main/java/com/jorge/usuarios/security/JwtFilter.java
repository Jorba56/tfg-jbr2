package com.jorge.usuarios.security;

import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
/**
 * Filtro de seguridad personalizado que se ejecuta una vez por cada petición HTTP.
 * Se encarga de interceptar las peticiones, extraer el token JWT de la cabecera "Authorization",
 * validarlo y establecer el contexto de autenticación del usuario en Spring Security.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    /**
     * Constructor del filtro JWT.
     *
     * @param jwtUtil Utilidad para la validación y extracción de datos del token.
     * @param userRepository Repositorio para consultar los datos del usuario en la base de datos.
     */
    public JwtFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }
    /**
     * Método principal del filtro que procesa la petición HTTP.
     * Extrae el token Bearer, verifica su validez y, si es correcto, carga los roles
     * actualizados del usuario desde la base de datos para inyectarlos en el SecurityContext.
     *
     * @param request La petición HTTP entrante.
     * @param response La respuesta HTTP saliente.
     * @param filterChain Cadena de filtros para continuar la ejecución tras la validación.
     * @throws ServletException Si ocurre un error durante el procesamiento del servlet.
     * @throws IOException Si ocurre un error de entrada/salida.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String email = jwtUtil.validarTokenYObtenerEmail(token);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // buscamos al usuario en la BD para saber sus roles reales
                    User usuario = userRepository.findUserByEmailUsuario(email);

                    if (usuario != null) {
                        // convertimos roles de la bd al formato spring security
                        List<SimpleGrantedAuthority> authorities = usuario.getRoles().stream()
                                .map(rol -> new SimpleGrantedAuthority(rol.getName().toUpperCase()))
                                .toList();

                        // metemos los roles en el contexto de seguridad
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception e) {
                // para token inválido o expirado
                e.printStackTrace();
            }
        }
            filterChain.doFilter(request, response);
        }
    }

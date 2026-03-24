package com.jorge.usuarios.exceptions;

import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Interceptor global de excepciones (Spring Advice) que captura cualquier error no controlado
 * lanzado desde los controladores o servicios. Formatea la salida de error en un JSON estándar
 * y registra la incidencia automáticamente en la base de datos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final UserRepository userRepository; // para sacar el id

    String badR = "Bad Request";
    String unauthorized = "Unauthorized";

    public GlobalExceptionHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("[INCIDENCIA_SEGURIDAD] Intento de acceso bloqueado.");
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", "Intento de acceso bloqueado por falta de permisos.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthenticationException(AuthenticationException ex) {
        log.warn("[ALERTA_SEGURIDAD] Intento de acceso sin token o con token inválido.");
        return buildResponse(HttpStatus.UNAUTHORIZED, unauthorized, "No estás autenticado o el token proporcionado no es válido.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("[ALERTA_LOGIN] Credenciales incorrectas.");

        return buildResponse(HttpStatus.UNAUTHORIZED, unauthorized, "Intento de inicio de sesión fallido.");
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException ex) {
        registrarIncidencia(ex);
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    public ResponseEntity<Object> handleBadRequest(Exception ex) {
        registrarIncidencia(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, badR, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException ex) {
        registrarIncidencia(ex);
        return buildResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<Object> handleDuplicate(DuplicateException ex) {
        registrarIncidencia(ex);
        return buildResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(org.springframework.security.authentication.InsufficientAuthenticationException.class)
    public org.springframework.http.ResponseEntity<Object> handleSecurityException(org.springframework.security.authentication.InsufficientAuthenticationException ex) {

        // imprimimos el aviso en la consola de Usuarios (usando el logger nativo permitido)
        java.util.logging.Logger.getLogger(this.getClass().getName())
                .warning("[ALERTA_SEGURIDAD] Intento de acceso sin token o con token inválido.");

        // como no hay token, el idUsuario será 0.
        registrarIncidencia(ex);

        //le devolvemos un mensaje al swagger
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("status", 401);
        body.put("error", "Unauthorized");
        body.put("message", "Necesitas estar logueado para acceder a este recurso.");

        return new org.springframework.http.ResponseEntity<>(body, org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        registrarIncidencia(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, badR, "Campos vacíos o formato incorrecto");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleTypeMismatch(HttpMessageNotReadableException ex) {
        registrarIncidencia(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, badR, "El tipo de dato introducido no coincide con el esperado en el JSON");
    }

    // métodos auxiliares

    private ResponseEntity<Object> buildResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }

    /**
     * Extrae automáticamente toda la información de la petición y de la Excepción
     * para rellenar la Incidencia y guardarla en Base de Datos.
     */
    private void registrarIncidencia(Exception ex) {
        String endpoint = "Desconocido";
        Long idUsuario = 0L;

        //extraer endpoint
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            endpoint = attrs.getRequest().getRequestURI();
        }

        //extraer id del usuario (si está logueado)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal()!=null && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            User user = userRepository.findUserByEmailUsuario(email);
            if (user != null) {
                idUsuario = user.getIdUser();
            }
        }

        //extraer detalles exactos de la excepción
        String tipo = ex.getClass().getSimpleName();
        String clase = "Desconocida";
        String metodo = "Desconocido";

        // cogemos la primera línea del stacktrace (donde se originó el fallo)
        if (ex.getStackTrace() != null && ex.getStackTrace().length > 0) {
            StackTraceElement elemento = ex.getStackTrace()[0];
            clase = elemento.getClassName();
            metodo = elemento.getMethodName();
        }

        if (clase.contains(".")) {
            clase = clase.substring(clase.lastIndexOf(".") + 1);
        }

        //limpiar proxys de spring en la clase (ej: userservice$$springcglib$$0 -> userservice)
        if (clase.contains("$$")) {
            clase = clase.substring(0, clase.indexOf("$$"));
        }

        // limpiar lambdas en el metodo
        if (metodo.startsWith("lambda$")) {
            // separamos por el símbolo '$' y nos quedamos con la segunda parte
            metodo = metodo.split("\\$")[1];
        }

        //extraer la traza completa (la pasamos a string)
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String trazaCompleta = sw.toString();

        // recortamos la traza si es larga para proteger la base de datos
        String traza = trazaCompleta.length() > 2000 ? trazaCompleta.substring(0, 2000) : trazaCompleta;

        //creamos un map (diccionario) para representar el json
        java.util.Map<String, Object> incidenciaJson = new java.util.HashMap<>();
        incidenciaJson.put("endpoint", endpoint);
        incidenciaJson.put("tipo", tipo);
        incidenciaJson.put("clase", clase);
        incidenciaJson.put("metodo", metodo);
        incidenciaJson.put("traza", traza);
        incidenciaJson.put("fecha", LocalDateTime.now().toString());

        // AQUÍ ESTÁ EL ARREGLO PARA EL NULL
        incidenciaJson.put("id_usuario", idUsuario);

        // la magia de microservicios: hacemos una petición post al otro microservicio
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        try {
            restTemplate.postForObject("http://host.docker.internal:8082/incidencias", incidenciaJson, String.class);
        } catch (Exception e) {
            // Usamos logger nativo para asegurar la compilación
            java.util.logging.Logger.getLogger(this.getClass().getName())
                    .severe("No se pudo comunicar con el servidor de incidencias: " + e.getMessage());
        }
    }
}
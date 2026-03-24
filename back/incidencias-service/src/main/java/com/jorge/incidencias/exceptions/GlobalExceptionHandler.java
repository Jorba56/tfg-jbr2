package com.jorge.incidencias.exceptions;

import com.jorge.incidencias.entity.Incidencia;
import com.jorge.incidencias.services.IncidenciasService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private final IncidenciasService incidenciaService;

    // Solo inyectamos IncidenciasService (aquí no existe UserRepository)
    public GlobalExceptionHandler(IncidenciasService incidenciaService) {
        this.incidenciaService = incidenciaService;
    }

    /**
     * Captura cualquier excepción genérica (Exception) no controlada por otros manejadores.
     * Registra el error en la base de datos y devuelve una respuesta HTTP 500 (Internal Server Error).
     *
     * @param ex La excepción capturada.
     * @return Respuesta estandarizada en formato JSON.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(Exception ex) {
        registrarIncidencia(ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Interno", "Ha ocurrido un error inesperado.");
    }

    /**
     * Maneja las excepciones personalizadas de tipo NotFoundException.
     * Registra el suceso y devuelve una respuesta HTTP 404 (Not Found).
     *
     * @param ex La excepción NotFoundException capturada.
     * @return Respuesta estandarizada detallando el recurso no encontrado.
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException ex) {
        registrarIncidencia(ex);
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    private ResponseEntity<Object> buildResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }

    /**
     * Extrae de forma reflexiva los metadatos de la excepción (endpoint, clase, método y stacktrace).
     * Limpia los nombres de clases generadas dinámicamente o lambdas para una mayor legibilidad,
     * trunca la traza a 2000 caracteres para evitar desbordamientos y persiste la incidencia.
     *
     * @param ex La excepción de la cual se extraerá el contexto.
     */
    private void registrarIncidencia(Exception ex) {
        String endpoint = "Desconocido";

        // Como aquí no tenemos acceso a la base de datos de usuarios, asignamos 0L temporalmente.
        Long idUsuario = 0L;

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            endpoint = attrs.getRequest().getRequestURI();
        }

        String tipo = ex.getClass().getSimpleName();
        String clase = "Desconocida";
        String metodo = "Desconocido";

        if (ex.getStackTrace() != null && ex.getStackTrace().length > 0) {
            StackTraceElement elemento = ex.getStackTrace()[0];
            clase = elemento.getClassName();
            metodo = elemento.getMethodName();
        }

        if (clase.contains(".")) {
            clase = clase.substring(clase.lastIndexOf(".") + 1);
        }

        if (clase.contains("$$")) {
            clase = clase.substring(0, clase.indexOf("$$"));
        }

        if (metodo.startsWith("lambda$")) {
            metodo = metodo.split("\\$")[1];
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String trazaCompleta = sw.toString();
        String traza = trazaCompleta.length() > 2000 ? trazaCompleta.substring(0, 2000) : trazaCompleta;

        // Aquí SÍ guardamos usando el objeto Incidencia y el servicio directamente
        Incidencia incidencia = new Incidencia();
        incidencia.setEndpoint(endpoint);
        incidencia.setTipo(tipo);
        incidencia.setClase(clase);
        incidencia.setMetodo(metodo);
        incidencia.setTraza(traza);
        incidencia.setFecha(LocalDateTime.now());
        incidencia.setIdUsuario(idUsuario);

        incidenciaService.guardar(incidencia);
    }
}
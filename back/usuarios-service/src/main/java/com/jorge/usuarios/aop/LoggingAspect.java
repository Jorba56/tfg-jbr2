package com.jorge.usuarios.aop;

import com.jorge.usuarios.exceptions.BadRequestException;
import com.jorge.usuarios.exceptions.DuplicateException;
import com.jorge.usuarios.exceptions.NotFoundException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    //logger de SLF4J
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("within(com.jorge.sprintdef.services..*)")
    public void vigilarServicios() {}

    @Before("vigilarServicios()")
    public void logAntesDeEntrar(JoinPoint joinPoint) {
        log.info("Ejecutando: {}", joinPoint.getSignature().getName());
    }

    @AfterReturning(pointcut = "vigilarServicios()", returning = "resultado")
    public void logDespuesDeSalir(JoinPoint joinPoint, Object resultado) {
        String nombreMetodo = joinPoint.getSignature().getName();

        if (resultado instanceof String string) {
            String respuesta =  string.toLowerCase();

            if (respuesta.contains("error") || respuesta.contains("no encontrado") || respuesta.contains("no válido")) {
                String mensaje = "Rechazado en " + nombreMetodo + " | Motivo: " + resultado;
                log.warn("[AVISO_NEGOCIO] {}", mensaje);
                return;
            }
        }

        log.info("Finalizado con éxito: {}", nombreMetodo);
    }

    @AfterThrowing(pointcut = "vigilarServicios()", throwing = "error")
    public void logEnCasoDeError(JoinPoint joinPoint, Throwable error) {
        String mensaje = "Fallo en: " + joinPoint.getSignature().getName() + " | Causa: " + error.getMessage();
        String tipoError = "ERROR_SISTEMA";

        if (error instanceof NotFoundException) tipoError = "ERROR_NO_ENCONTRADO";
        else if (error instanceof DuplicateException) tipoError = "ERROR_DUPLICADO";
        else if (error instanceof BadRequestException) tipoError = "ERROR_PETICION_INVALIDA";

        log.error("[{}] {}", tipoError, mensaje);
    }

}
package com.jorge.incidencias.controller;

import com.jorge.incidencias.entity.Incidencia;
import com.jorge.incidencias.services.IncidenciasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST que expone los endpoints para la consulta del registro de incidencias y errores.
 * Por motivos de seguridad y auditoría, el acceso a estos endpoints está estrictamente restringido
 * a usuarios con el rol de Administrador.
 */
@RestController
@RequestMapping("/incidencias")
@CrossOrigin(origins = {"http://localhost:8081", "http://localhost:8082"})
@Tag(name = "Incidencias", description = "Endpoints para consultar el registro de errores y excepciones del sistema.")
public class IncidenciasController {

    private final IncidenciasService incidenciasService;

    public IncidenciasController(IncidenciasService incidenciasService) {
        this.incidenciasService = incidenciasService;
    }

    /**
     * Consulta el listado completo de todas las incidencias registradas.
     */
    @Operation(summary = "Listar historial de incidencias", description = "Devuelve todas las incidencias (errores de código, accesos denegados, etc.) guardadas en la base de datos.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public List<Incidencia> getAllIncidencias() {
        return incidenciasService.obtenerTodas();
    }

    /**
     * Registra una nueva incidencia (error de login, registro, examen, etc.) en el sistema.
     */
    @Operation(summary = "Registrar incidencia manual", description = "Permite enviar un reporte de incidencia manualmente desde el cliente al servidor.")
    @PostMapping("")
    public String createIncidencia(@RequestBody Incidencia incidencia) {
        incidenciasService.guardar(incidencia);
        return "Incidencia guardada correctamente.";
    }

    /**
     * Busca los detalles de una incidencia específica por su ID.
     */
    @Operation(summary = "Buscar incidencia por ID", description = "Obtiene los detalles de una única incidencia buscada por su identificador.")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('ADMINISTRADOR')")
    @GetMapping("/{id}")
    public Incidencia getIncidenciaById(@PathVariable Long id) {
        return incidenciasService.obtenerPorId(id);
    }

    @Operation(summary = "Buscar incidencias por Usuario", description = "Devuelve todas las incidencias asociadas a un ID de usuario específico.")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('ADMINISTRADOR')")
    @GetMapping("/usuario/{idUsuario}")
    public List<Incidencia> getIncidenciasByUsuario(@PathVariable Long idUsuario) {
        return incidenciasService.obtenerPorUsuario(idUsuario);
    }

    @Operation(summary = "Buscar incidencias por Clase", description = "Filtra las incidencias buscando en qué clase de Java se originaron (Ej: UserService).")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('ADMINISTRADOR')")
    @GetMapping("/clase/{clase}")
    public List<Incidencia> getIncidenciasByClase(@PathVariable String clase) {
        return incidenciasService.obtenerPorClase(clase);
    }

    @Operation(summary = "Buscar incidencias por Método", description = "Filtra las incidencias buscando en qué método específico de Java se originaron (Ej: buscarPorId).")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('ADMINISTRADOR')")
    @GetMapping("/metodo/{metodo}")
    public List<Incidencia> getIncidenciasByMetodo(@PathVariable String metodo) {
        return incidenciasService.obtenerPorMetodo(metodo);
    }

}
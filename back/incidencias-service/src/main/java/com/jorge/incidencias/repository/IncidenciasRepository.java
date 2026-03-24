package com.jorge.incidencias.repository;

import com.jorge.incidencias.entity.Incidencia;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de Spring Data JPA encargado de la persistencia de la entidad {@link Incidencia}.
 * Proporciona operaciones CRUD estándar y consultas derivadas personalizadas para filtrar
 * el historial de auditoría de errores del sistema.
 */
@Repository
public interface IncidenciasRepository extends JpaRepository<Incidencia, Long> {
    /**
     * Busca todas las incidencias que se originaron en una clase Java concreta.
     * @param clase El nombre de la clase (ej. "UserService").
     * @return Lista de incidencias correspondientes.
     */
    List<Incidencia> findByClase(String clase);

    /**
     * Recupera el historial de errores generados por un usuario específico.
     * @param id El identificador único del usuario.
     * @return Lista de incidencias asociadas a dicho usuario.
     */
    List<Incidencia> findByIdUsuario(Long id);

    /**
     * Busca todas las incidencias que fueron lanzadas desde un método específico.
     * @param metodo El nombre del método donde ocurrió la excepción.
     * @return Lista de incidencias originadas en ese método.
     */
    List<Incidencia> findByMetodo(String metodo);
}


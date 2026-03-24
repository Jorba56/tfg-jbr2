package com.jorge.incidencias.services;

import com.jorge.incidencias.entity.Incidencia;

import java.util.List;

/**
 * Interfaz que define las operaciones de negocio para la gestión del registro de incidencias.
 */
public interface IncidenciasService {

    /**
     * Obtiene todas las incidencias registradas en el sistema.
     *
     * @return Lista completa del historial de incidencias.
     */
    List<Incidencia> obtenerTodas();

    /**
     * Guarda una nueva incidencia generada en la base de datos.
     *
     * @param incidencia La entidad de la incidencia a persistir.
     */
    void guardar(Incidencia incidencia);

    /**
     * Busca los detalles de una incidencia por su identificador único.
     *
     * @param id Identificador de la incidencia.
     * @return La incidencia encontrada.
     */
    Incidencia obtenerPorId(Long id);

    /**
     * Obtiene todas las incidencias asociadas a las peticiones de un usuario específico.
     *
     * @param idUsuario Identificador del usuario.
     * @return Lista de incidencias provocadas por el usuario.
     */
    List<Incidencia> obtenerPorUsuario(Long idUsuario);

    /**
     * Obtiene todas las incidencias que se originaron en una clase de Java específica.
     *
     * @param clase Nombre de la clase donde ocurrió el error (ej: UserService).
     * @return Lista de incidencias correspondientes a esa clase.
     */
    List<Incidencia> obtenerPorClase(String clase);

    /**
     * Obtiene todas las incidencias originadas en un método específico.
     *
     * @param metodo Nombre del método donde ocurrió el error.
     * @return Lista de incidencias correspondientes a ese método.
     */
    List<Incidencia> obtenerPorMetodo(String metodo);
}

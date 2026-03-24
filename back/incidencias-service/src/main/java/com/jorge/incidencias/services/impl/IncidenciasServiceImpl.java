package com.jorge.incidencias.services.impl;

import com.jorge.incidencias.entity.Incidencia;
import com.jorge.incidencias.repository.IncidenciasRepository; // Interfaz que extiende de JpaRepository<Incidencia, Long>
import com.jorge.incidencias.exceptions.NotFoundException;
import com.jorge.incidencias.services.IncidenciasService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio encargado de la lógica de negocio para la gestión y consulta de incidencias del sistema.
 * Proporciona métodos para registrar errores automáticos y consultarlos mediante distintos filtros.
 */
@Service
public class IncidenciasServiceImpl implements IncidenciasService {

    private final IncidenciasRepository incidenciaRepository;

    public IncidenciasServiceImpl(IncidenciasRepository incidenciaRepository) {
        this.incidenciaRepository = incidenciaRepository;
    }

    /**
     * Recupera todas las incidencias registradas en el sistema.
     *
     * @return Una lista que contiene todas las entidades {@link Incidencia} almacenadas.
     */
    @Override
    public List<Incidencia> obtenerTodas() {
        return incidenciaRepository.findAll();
    }

    /**
     * Guarda una nueva incidencia en la base de datos.
     *
     * @param incidencia La entidad {@link Incidencia} generada, habitualmente desde el GlobalExceptionHandler.
     */
    @Override
    public void guardar(Incidencia incidencia) {
        incidenciaRepository.save(incidencia);
    }

    /**
     * Busca y recupera una incidencia específica utilizando su identificador único.
     *
     * @param id Identificador numérico de la incidencia a buscar.
     * @return La entidad {@link Incidencia} encontrada.
     * @throws NotFoundException Si no se encuentra ninguna incidencia con el identificador proporcionado.
     */
    @Override
    public Incidencia obtenerPorId(Long id) {
        return incidenciaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No se ha encontrado ninguna incidencia con el ID: " + id));
    }

    /**
     * Recupera el historial de incidencias vinculadas a un usuario en particular.
     *
     * @param idUsuario Identificador del usuario que originó las incidencias (basado en su token en el momento del error).
     * @return Lista de incidencias provocadas o relacionadas con dicho usuario.
     * @throws NotFoundException Si el usuario indicado no tiene ninguna incidencia registrada.
     */
    @Override
    public List<Incidencia> obtenerPorUsuario(Long idUsuario) {
        List<Incidencia> incidencias = incidenciaRepository.findByIdUsuario(idUsuario);
        if (incidencias.isEmpty()) {
            throw new NotFoundException("No se han encontrado incidencias para el usuario con ID: " + idUsuario);
        }
        return incidencias;
    }

    /**
     * Filtra y recupera las incidencias que se hayan originado dentro de una clase de Java específica.
     *
     * @param clase El nombre de la clase (ej. "UserService") donde ocurrió la excepción.
     * @return Lista de incidencias producidas en esa clase.
     * @throws NotFoundException Si no existe ningún registro de incidencia en la clase solicitada.
     */
    @Override
    public List<Incidencia> obtenerPorClase(String clase) {
        List<Incidencia> incidencias = incidenciaRepository.findByClase(clase);
        if (incidencias.isEmpty()) {
            throw new NotFoundException("No se han encontrado incidencias originadas en la clase: " + clase);
        }
        return incidencias;
    }

    /**
     * Filtra y recupera las incidencias que se hayan originado dentro de un método concreto.
     *
     * @param metodo El nombre del método (ej. "actualizarUsuario") donde ocurrió la excepción.
     * @return Lista de incidencias producidas en dicho método.
     * @throws NotFoundException Si no se han registrado incidencias con ese nombre de método.
     */
    @Override
    public List<Incidencia> obtenerPorMetodo(String metodo) {
        List<Incidencia> incidencias = incidenciaRepository.findByMetodo(metodo);
        if (incidencias.isEmpty()) {
            throw new NotFoundException("No se han encontrado incidencias originadas en el método: " + metodo);
        }
        return incidencias;
    }
}
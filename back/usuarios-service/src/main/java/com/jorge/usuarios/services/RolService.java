package com.jorge.usuarios.services;

import com.jorge.usuarios.dto.RolDTO;
import com.jorge.usuarios.dto.RolPutDTO;
import com.jorge.usuarios.dto.UserByRol;
import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.exceptions.ConflictException;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz que define las operaciones de negocio para la gestión de los Roles de acceso.
 */
public interface RolService {
    /**
     * Lista todos los roles activos en el sistema.
     *
     * @return Lista de entidades de roles activos.
     */
    List<Rol> listarRoles();

    /**
     * Busca un rol activo por su identificador.
     *
     * @param id Identificador del rol.
     * @return Un Optional con el rol encontrado.
     */
    Optional<Rol> rolPorId(Long id);

    /**
     * Crea un nuevo rol en el sistema.
     *
     * @param rol DTO con los datos básicos del nuevo rol.
     * @return Mensaje de confirmación de la creación.
     */
    String newRol(RolDTO rol);

    /**
     * Actualiza los datos de un rol existente.
     *
     * @param id Identificador del rol a actualizar.
     * @param rolNuevo DTO con los nuevos datos (nombre y estado).
     * @return Mensaje indicando el resultado de la operación.
     */
    String actualizarRol(Long id, RolPutDTO rolNuevo);

    /**
     * Desactiva un rol en el sistema (borrado lógico).
     *
     * @param id Identificador del rol a desactivar.
     * @return Mensaje de confirmación.
     * @throws ConflictException Si el rol ya está desactivado o tiene usuarios asignados.
     */
    String desactivarRol(Long id) throws ConflictException;

    /**
     * Mapea un objeto de transferencia RolDTO a una entidad Rol.
     *
     * @param rol DTO con los datos del rol.
     * @return Entidad Rol mapeada y lista para persistir.
     */
    Rol mappingARol(RolDTO rol);

    /**
     * Obtiene la lista de usuarios que poseen un rol determinado.
     *
     * @param idRol Identificador del rol consultado.
     * @return Lista de usuarios (en formato DTO) que tienen el rol asignado.
     */
    List<UserByRol> userPorRol(Long idRol);
}

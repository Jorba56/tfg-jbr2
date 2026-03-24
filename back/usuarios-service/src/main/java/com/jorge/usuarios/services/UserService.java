package com.jorge.usuarios.services;

import com.jorge.usuarios.dto.RolPostUser;
import com.jorge.usuarios.dto.UserAddDTO;
import com.jorge.usuarios.dto.UserIdDTo;
import com.jorge.usuarios.dto.UsersAllDTO;
import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.exceptions.BadRequestException;
import com.jorge.usuarios.exceptions.DuplicateException;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Interfaz que define las operaciones de negocio para la gestión de Usuarios.
 */
public interface UserService {
    /**
     * Obtiene una lista de todos los usuarios activos en el sistema.
     *
     * @return Lista de usuarios proyectados a DTO de vista general.
     */
    List<UsersAllDTO> listarUsuarios();

    /**
     * Busca un usuario activo por su identificador.
     *
     * @param id Identificador del usuario.
     * @return DTO con la información detallada del usuario.
     */
    UserIdDTo buscarPorId(Long id);

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param usuario DTO con los datos de registro del usuario.
     * @return DTO con los datos del usuario recién creado.
     * @throws DuplicateException Si el correo electrónico ya está registrado.
     */
    UsersAllDTO addUsuario(UserAddDTO usuario) throws DuplicateException;

    /**
     * Actualiza el perfil de un usuario existente, aplicando reglas de seguridad.
     *
     * @param id Identificador del usuario a modificar.
     * @param usuario Objeto con los nuevos datos a actualizar.
     * @param authentication Contexto de seguridad actual para validar permisos (ABAC).
     * @return Mensaje de confirmación de la actualización.
     * @throws BadRequestException Si se incumplen reglas de negocio o permisos de edición.
     */
    String actualizarUsuario(Long id, User usuario, Authentication authentication) throws BadRequestException;

    /**
     * Desactiva un usuario del sistema (borrado lógico).
     *
     * @param id Identificador del usuario a desactivar.
     * @return Mensaje de confirmación.
     */
    String desactivarUsuario(Long id);

    /**
     * Obtiene la lista de roles asignados a un usuario específico.
     *
     * @param idUser Identificador del usuario.
     * @return Lista de roles del usuario.
     */
    List<Rol> rolesUser(Long idUser);

    /**
     * Asigna un rol existente a un usuario.
     *
     * @param idUser Identificador del usuario.
     * @param idRol DTO con el identificador del rol a añadir.
     * @return Mensaje de confirmación.
     * @throws DuplicateException Si el usuario ya posee dicho rol.
     */
    String addRolUser(Long idUser, RolPostUser idRol) throws DuplicateException;

    /**
     * Revoca un rol asignado a un usuario.
     *
     * @param idUser Identificador del usuario.
     * @param idRol Identificador del rol a retirar.
     * @return Mensaje de confirmación.
     */
    String deleteRolUser(Long idUser, Long idRol);
}

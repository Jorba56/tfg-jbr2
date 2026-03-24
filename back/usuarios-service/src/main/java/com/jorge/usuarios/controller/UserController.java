package com.jorge.usuarios.controller;

import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.dto.RolPostUser;
import com.jorge.usuarios.dto.UserIdDTo;
import com.jorge.usuarios.dto.UsersAllDTO;

import com.jorge.usuarios.exceptions.BadRequestException;
import com.jorge.usuarios.exceptions.DuplicateException;
import com.jorge.usuarios.services.impl.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;

/**
 * Controlador REST encargado de gestionar las peticiones HTTP relacionadas con los Usuarios.
 * Define los endpoints para el CRUD de usuarios y la gestión de sus roles asignados.
 */
@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuarios", description = "Endpoints para el CRUD de usuarios y la gestión de sus roles asignados.")
public class UserController {

    private final UserServiceImpl userServiceImpl;

    public UserController(UserServiceImpl userServiceImpl) {
        this.userServiceImpl = userServiceImpl;
    }

    /**
     * Obtiene la lista completa de usuarios activos en el sistema.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Listar todos los usuarios", description = "Obtiene una lista con la información pública de todos los usuarios activos en el sistema.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public List<UsersAllDTO> getAllUsers() {
        return userServiceImpl.listarUsuarios();
    }

    /**
     * Busca y devuelve los datos de un usuario específico mediante su ID.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Buscar usuario por ID", description = "Devuelve los detalles completos de un usuario específico. Solo accesible para administradores.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{id}")
    public UserIdDTo getUserId(@PathVariable Long id) {
        return userServiceImpl.buscarPorId(id);
    }

    /**
     * Endpoint para actualizar los datos de un usuario en el sistema.
     * Protegido por autenticación JWT. La lógica de negocio determina los permisos exactos basándose en el token.
     *
     * @param id Identificador del usuario a modificar, obtenido de la ruta (URL).
     * @param usuario Objeto JSON recibido en el cuerpo de la petición con los nuevos datos.
     * @param authentication Información de sesión inyectada automáticamente por Spring Security.
     * @return Cadena de texto confirmando la edición exitosa.
     */
    @Operation(summary = "Actualizar usuario", description = "Modifica los datos de un usuario. Bloquea la edición de contraseñas para admins y roles para usuarios normales.")
    @PutMapping("/{id}")
    public String updateUser(@PathVariable Long id, @RequestBody User usuario, Authentication authentication) throws BadRequestException {
        return userServiceImpl.actualizarUsuario(id, usuario, authentication);
    }

    /**
     * Realiza un borrado lógico del usuario especificado.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Desactivar usuario", description = "Realiza un borrado lógico del usuario especificado cambiando su estado activo a false.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public String deleteUser (@PathVariable Long id){
        return userServiceImpl.desactivarUsuario(id);
    }

    /**
     * Obtiene la lista de roles que tiene asignados un usuario en concreto.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Ver roles de un usuario", description = "Obtiene la lista de los roles de seguridad que tiene asignados un usuario en concreto.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{id}/roles")
    public List<Rol> rolesUser(@PathVariable Long id){
        return userServiceImpl.rolesUser(id);
    }

    /**
     * Asigna un nuevo rol a un usuario existente.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Añadir rol a un usuario", description = "Asigna un nuevo rol a la lista de roles del usuario.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/{id}/roles")
    public String userAddRol(@PathVariable Long id, @RequestBody RolPostUser idRol) throws DuplicateException {
        return userServiceImpl.addRolUser(id,idRol);
    }

    /**
     * Revoca (elimina) un rol específico de un usuario.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Revocar rol a un usuario", description = "Elimina la asociación de un rol específico con un usuario.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{idUsuario}/roles/{idRol}") //Quitarle un rol a un usuario
    public String deleteRolUser (@PathVariable Long idRol, @PathVariable Long idUsuario) {
        return userServiceImpl.deleteRolUser(idUsuario, idRol );
    }
}

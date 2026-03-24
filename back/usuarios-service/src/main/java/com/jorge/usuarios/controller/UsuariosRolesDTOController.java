package com.jorge.usuarios.controller;

import com.jorge.usuarios.dto.UsuarioRolesDTO;
import com.jorge.usuarios.services.UsuariosRolesDTOService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controlador REST que expone los endpoints para consultar la información combinada de usuarios y sus roles.
 * Utiliza Data Transfer Objects (DTOs) para devolver la información de forma plana y optimizada.
 * Por motivos de seguridad, el acceso a estas rutas está estrictamente protegido y reservado para administradores.
 */
@RestController
@RequestMapping("/usuarios_roles")
@Tag(name = "Relación Usuarios-Roles", description = "Endpoints para consultar la información combinada de los usuarios y sus roles de forma plana.")
public class UsuariosRolesDTOController {

    private final UsuariosRolesDTOService urService;

    public UsuariosRolesDTOController(UsuariosRolesDTOService urService) {
        this.urService = urService;
    }

    /**
     * Recupera una lista de todos los usuarios activos en el sistema junto con los identificadores
     * numéricos de los roles que tienen asignados en ese momento.
     * Este endpoint es de acceso exclusivo para usuarios con la autoridad 'ADMIN'.
     *
     * @return Una lista de objetos {@link UsuarioRolesDTO} que contienen el ID del usuario y la lista de IDs de sus roles.
     */
    @Operation(summary = "Listar usuarios con sus roles", description = "Devuelve una lista que combina los datos básicos de los usuarios con los detalles de los roles que tienen asignados en el sistema.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public List<UsuarioRolesDTO> getAllRoles() {
        return urService.listaUsuariosRoles();
    }
}

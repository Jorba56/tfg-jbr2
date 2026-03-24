package com.jorge.usuarios.services;

import com.jorge.usuarios.dto.UsuarioRolesDTO;

import java.util.List;

/**
 * Interfaz que define las operaciones para obtener vistas combinadas y aligeradas de usuarios y sus roles.
 */
public interface UsuariosRolesDTOService {
    /**
     * Obtiene un listado de todos los usuarios activos y extrae de forma limpia
     * los identificadores de sus roles asociados.
     *
     * @return Lista de DTOs que relacionan el ID del usuario con una lista de IDs de sus roles.
     */
    List<UsuarioRolesDTO> listaUsuariosRoles();
}

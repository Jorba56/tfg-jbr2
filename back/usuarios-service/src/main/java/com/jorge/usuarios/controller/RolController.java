package com.jorge.usuarios.controller;

import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.dto.RolDTO;
import com.jorge.usuarios.dto.RolPutDTO;
import com.jorge.usuarios.dto.UserByRol;
import com.jorge.usuarios.exceptions.ConflictException;
import com.jorge.usuarios.services.impl.RolServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

/**
 * Controlador REST encargado de gestionar las peticiones HTTP relacionadas con los Roles del sistema.
 * Permite listar, crear, modificar y eliminar los distintos niveles de acceso (roles).
 */
@RestController
@RequestMapping("/roles")
@Tag(name = "Roles", description = "Endpoints para la creación, modificación y listado de los niveles de acceso del sistema.")
public class RolController{

    private final RolServiceImpl rolServiceImpl;


    public RolController(RolServiceImpl rolServiceImpl) {
        this.rolServiceImpl = rolServiceImpl;
    }

    /**
     * Obtiene el listado de todos los roles disponibles y activos.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Listar todos los roles", description = "Obtiene el listado completo de roles activos disponibles para asignar.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public List<Rol> getAllRoles(){
        return rolServiceImpl.listarRoles();
    }

    /**
     * Busca un rol específico por su identificador.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Buscar rol por ID", description = "Devuelve la información de un rol específico mediante su identificador.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{id}")
    public Optional<Rol> getRolId (@PathVariable Long id){
        return rolServiceImpl.rolPorId(id);
    }

    /**
     * Crea un nuevo rol en el sistema (por ejemplo, "ADMIN" o "PROFESOR").
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Crear nuevo rol", description = "Registra un nuevo rol (ej: PROFESOR, ADMIN) en la base de datos.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("")
    public String addRol (@RequestBody RolDTO rol){
        return rolServiceImpl.newRol(rol);
    }

    /**
     * Modifica el nombre o el estado de un rol existente.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Actualizar rol", description = "Modifica el nombre o el estado de un rol ya existente.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}")
    public String updateRol(@PathVariable Long id, @RequestBody RolPutDTO rolNuevo) {
        return rolServiceImpl.actualizarRol(id, rolNuevo);
    }

    /**
     * Realiza el borrado lógico de un rol para que deje de estar disponible.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Desactivar rol", description = "Realiza un borrado lógico del rol. Fallará si el rol tiene usuarios asignados.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public String deleteRol (@PathVariable Long id) throws ConflictException {
        return rolServiceImpl.desactivarRol(id);
    }

    /**
     * Consulta qué usuarios tienen asignado el rol especificado.
     * Exclusivo para el administrador.
     */
    @Operation(summary = "Ver usuarios por rol", description = "Devuelve una lista con todos los usuarios que poseen un rol determinado.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{idRol}/usuarios")
    public List<UserByRol> userRol(@PathVariable Long idRol){
        return rolServiceImpl.userPorRol(idRol);
    }

}

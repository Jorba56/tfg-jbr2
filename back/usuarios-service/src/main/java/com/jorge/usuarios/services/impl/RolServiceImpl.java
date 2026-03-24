package com.jorge.usuarios.services.impl;


import com.jorge.usuarios.dto.RolDTO;
import com.jorge.usuarios.dto.RolPutDTO;
import com.jorge.usuarios.dto.UserByRol;
import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.exceptions.*;
import com.jorge.usuarios.mapping.RolMapper;
import com.jorge.usuarios.mapping.UserMapper;
import com.jorge.usuarios.repository.RolRepository;
import com.jorge.usuarios.services.RolService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio encargado de gestionar la lógica de negocio relacionada con los Roles.
 * Actúa como intermediario entre el controlador y la base de datos,
 * procesando las transformaciones de DTOs y validando las reglas de negocio.
 */
@Service
public class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;
    private final RolMapper rolMap;
    private final UserMapper userMap;

    public RolServiceImpl(RolRepository rolRepository, RolMapper rolMap, UserMapper userMap) {
        this.rolMap = rolMap;
        this.userMap = userMap;
        this.rolRepository = rolRepository;
    }

    /**
     * Obtiene la lista de todos los roles que se encuentran activos en el sistema.
     *
     * @return Una lista de entidades Rol que tienen el flag 'activo' a true.
     */
    @Override
    public List<Rol>listarRoles(){
        return rolRepository.findRolsByActivoIs(true);
    }

    /**
     * Busca un rol específico en la base de datos a partir de su identificador.
     *
     * @param id El identificador único del rol a buscar.
     * @return Un Optional que contiene el rol si se encuentra, o vacío si no existe.
     */
    @Override
    public Optional<Rol> rolPorId(Long id){
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rol no encontrado con ID: " + id));

        if (!rol.getActivo()) {
            throw new NotFoundException("El rol se encuentra desactivado.");
        }
        return Optional.of(rol);
    }

    /**
     * Crea y guarda un nuevo rol en la base de datos a partir de los datos recibidos desde el cliente.
     *
     * @param rol Objeto DTO con la información básica del nuevo rol a crear.
     * @return Un mensaje de texto confirmando la creación exitosa del rol.
     */
    @Override
    public String newRol(RolDTO rol){
        Rol rol2=mappingARol(rol);
        rolRepository.save(rol2);
        return("Rol añadido con exito");
    }

    /**
     * Actualiza los datos de un rol existente en el sistema.
     *
     * @param id El identificador único del rol que se desea modificar.
     * @param rolNuevo Objeto DTO que contiene los nuevos datos (nombre y estado de activación).
     * @return Un mensaje confirmando la edición o indicando que el rol no fue encontrado.
     */
    @Override
    public String actualizarRol(Long id, RolPutDTO rolNuevo) {
         String salida;
        // 1. Buscamos el usuario y abrimos el Optional de forma segura
        Rol rolUpdate = rolRepository.findById(id).orElse(null);

        // 2. Comprobamos que exista
        if (rolUpdate==null) {
             salida= "Error: Rol no encontrado";
        }else {
            Rol rolUpdate2= rolMap.mappingPutReverse(rolNuevo);
            rolUpdate.setName(rolUpdate2.getName());
            rolUpdate.setActivo(rolUpdate2.getActivo());
            // 5.   Guardamos en la base de datos
            rolRepository.save(rolUpdate);
            salida= "Rol con id"+id+" editado correctamente";
        }
        return salida;
    }

    /**
     * Realiza un borrado lógico de un rol, cambiando su estado a inactivo para no perder el histórico.
     *
     * @param id El identificador único del rol a desactivar.
     * @return Un mensaje confirmando el borrado exitoso o un aviso si el rol no existe.
     */
    @Override
    public String desactivarRol(Long id) throws ConflictException {
        Rol rolSelect = rolRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rol no encontrado con ID: " + id));

        if (!rolSelect.getActivo()) {
            throw new ConflictException("El rol ya está desactivado.");
        }

        // Comprobar si hay usuarios asignados a este rol
        List<User> usuariosConRol = rolRepository.findUsuariosPorRol(id);
        if (!usuariosConRol.isEmpty()) {
            throw new ConflictException("No se puede desactivar un rol que tiene usuarios asignados.");
        }

        rolSelect.setActivo(false);
        rolRepository.save(rolSelect);
        return "Rol con id " + id + " borrado con éxito";
    }

    /**
     * Convierte un objeto de transferencia de datos (RolDTO) en una entidad Rol.
     *
     * @param rol El DTO que contiene los datos de entrada.
     * @return La entidad Rol lista para ser persistida en base de datos.
     */
    @Override
    public Rol mappingARol(RolDTO rol){
        Rol dto= new Rol();
        dto.setName(rol.getName());
        return dto;
    }

    /**
     * Obtiene una lista de todos los usuarios que tienen asignado un rol específico.
     * Transforma las entidades en DTOs para no exponer información sensible.
     *
     * @param idRol El identificador del rol a consultar.
     * @return Una lista de objetos UserByRol con la información pública de los usuarios.
     */
    @Override
    public List<UserByRol> userPorRol(Long idRol){
        List<UserByRol> respuesta= new ArrayList<>();
        List<User> users = rolRepository.findUsuariosPorRol(idRol);
        for (int i=0; i<users.size();i++){
            userMap.mappingRoles(users.get(i));
            respuesta.add(userMap.mappingRoles(users.get(i)));
        }
        return respuesta;
    }

}

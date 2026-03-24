package com.jorge.usuarios.services.impl;

import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.repository.RolRepository;
import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.dto.RolPostUser;
import com.jorge.usuarios.dto.UserAddDTO;
import com.jorge.usuarios.dto.UserIdDTo;
import com.jorge.usuarios.dto.UsersAllDTO;
import com.jorge.usuarios.repository.UserRepository;

import com.jorge.usuarios.exceptions.*;
import com.jorge.usuarios.mapping.UserMapper;
import com.jorge.usuarios.services.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Servicio encargado de gestionar la lógica de negocio relacionada con los Usuarios.
 * Actúa como intermediario entre el controlador y la base de datos,
 * procesando las transformaciones de DTOs y validando las reglas de negocio.
 */
@Service
public class UserServiceImpl implements UserService {

    private final RolRepository rolRep;
    private final UserRepository userRep;
    private final UserMapper userMap;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crea una nueva instancia del servicio de usuarios inyectando sus dependencias.
     * Inicializa el repositorio de usuarios, el mapper de usuarios y el repositorio de roles
     * necesarios para las operaciones del servicio.
     *
     * @param userRep repositorio para realizar operaciones de persistencia sobre usuarios
     * @param userMap mapper encargado de transformar entidades de usuario y sus DTOs
     * @param rolRep repositorio para la gestión de la persistencia de roles asociados a usuarios
     */
    public UserServiceImpl(UserRepository userRep, UserMapper userMap, RolRepository rolRep, PasswordEncoder passwordEncoder) {
        this.userRep = userRep;
        this.userMap = userMap;
        this.rolRep = rolRep;
        this.passwordEncoder = passwordEncoder;
    }

    String rolNoEncontrado="El rol introducido no existe en el sistema.";
    String usuarioNoEncontrado="El usuario introducido no existe en el sistema.";

    /**
     * Obtiene todos los usuarios que están activos y los proyecta a {@code UsersAllDTO}.
     *
     * @return lista de usuarios activos convertidos a {@code UsersAllDTO} para su correcta visualización; si no hay, una lista vacía
     */
    @Override
    public List<UsersAllDTO> listarUsuarios(){
        List<User> encontrados = userRep.findUsersByActivoIs(true);
        List<UsersAllDTO> usuarios=new ArrayList<>();
        for (User encontrado : encontrados) usuarios.add(userMap.mappingADTO(encontrado));
        return usuarios;
    }

    /**
     * Busca un usuario por su identificador y lo proyecta a {@code UserIdDTo}.
     * Si no existe, devuelve {@code null}.
     *
     * @param id identificador del usuario a consultar
     * @return representación {@code UserIdDTo} del usuario encontrado o {@code null} si no existe
     */
    @Override
    public UserIdDTo buscarPorId(Long id) {
        User usuarioEncontrado = userRep.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado con ID: " + id));

        if (!usuarioEncontrado.getActivo()) {
            throw new NotFoundException("El usuario con ID " + id + " está desactivado y no se puede mostrar.");
        }
        return userMap.userToIdDTO(usuarioEncontrado);
    }

    /**
     * Crea y persiste un nuevo usuario a partir de los datos proporcionados en {@code UserAddDTO}.
     * Devuelve la representación del usuario persistido como {@code UsersAllDTO}.
     *
     * @param usuario DTO con la información del usuario a crear
     * @return el usuario creado proyectado a {@code UsersAllDTO}
     */

    @Override
    public UsersAllDTO addUsuario(UserAddDTO usuario) throws DuplicateException {
        List <Rol> roles=new ArrayList<>();
        if ((userRep.findUserByEmailUsuario(usuario.getEmailUsuario()))!=null) {
            // Lanzamos una excepción controlada que luego podemos capturar para mostrar un error 400 al cliente
            throw new DuplicateException("El correo electrónico ya está en uso.");
        }

        User usuario2= userMap.userAddDTO(usuario);
        usuario2.setContrasenhaUsuario(passwordEncoder.encode(usuario.getContrasenhaUsuario()));

        Rol rolN=rolRep.findByName(("alumno")).orElseThrow(() -> new NotFoundException(rolNoEncontrado));
        roles.add(rolN);
        usuario2.setRoles(roles);
        userRep.save(usuario2);
        return(userMap.mappingADTO(usuario2));
    }

    /**
     * Actualiza los datos de un usuario existente aplicando control de acceso basado en el rol (ABAC).
     * Los administradores pueden modificar todos los campos excepto la contraseña.
     * Los usuarios normales solo pueden modificar su propio perfil, pudiendo cambiar su contraseña pero no sus roles.
     *
     * @param id Identificador único del usuario que se va a actualizar en la base de datos.
     * @param usuario Objeto con los nuevos valores a aplicar.
     * @param authentication Objeto de Spring Security que contiene los credenciales y roles del usuario que realiza la petición.
     * @return Mensaje de éxito indicando que el usuario ha sido editado correctamente.
     * @throws NotFoundException Si el usuario a actualizar no existe en la base de datos.
     * @throws BadRequestException Si un usuario normal intenta editar a otro, si intenta modificar sus roles, o si un admin intenta cambiar una contraseña.
     */
    @Override
    public String actualizarUsuario(Long id, User usuario, Authentication authentication) throws BadRequestException {
        User userUpdate = userRep.findById(id).orElseThrow(() -> new NotFoundException(usuarioNoEncontrado));

        String emailLogueado = authentication.getName();
        List<String> rolesAdmin = List.of("ADMIN", "ADMINISTRADOR", "ROLE_ADMIN");
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> rolesAdmin.contains(auth.getAuthority().toUpperCase()));

        if (!isAdmin && !userUpdate.getEmailUsuario().equals(emailLogueado)) {
            throw new BadRequestException("No tienes permisos para modificar el perfil de otro usuario.");
        }

        userUpdate.setNombreUsuario(usuario.getNombreUsuario());
        userUpdate.setApellidoUsuario(usuario.getApellidoUsuario());
        userUpdate.setEmailUsuario(usuario.getEmailUsuario());

        //simplificamos las condiciones para reducir complejidad
        boolean intentaCambiarPassword = usuario.getContrasenhaUsuario() != null && !usuario.getContrasenhaUsuario().trim().isEmpty();
        boolean intentaCambiarRoles = usuario.getRoles() != null && !usuario.getRoles().isEmpty();

        if (isAdmin) {
            if (intentaCambiarPassword) {
                throw new BadRequestException("Un administrador no puede cambiar la contraseña de un usuario.");
            }

            userUpdate.setActivo(usuario.getActivo()); // Solo el admin toca el estado activo

        } else {
            if (intentaCambiarRoles) {
                throw new BadRequestException("Un usuario que no es administrador no puede modificar sus roles.");
            }
            if (intentaCambiarPassword) {
                userUpdate.setContrasenhaUsuario(passwordEncoder.encode(usuario.getContrasenhaUsuario()));
            }
        }
        userRep.save(userUpdate);
        return "Usuario con id " + id + " editado correctamente";
        }

    /**
     * Desactiva (borrado lógico) un usuario estableciendo su campo {@code activo} a {@code false}.
     *
     * @param id identificador del usuario a desactivar
     * @return mensaje indicando si el usuario fue desactivado o si no existe
     */
    @Override
    public String desactivarUsuario(Long id){
        User userSelect=userRep.findById(id).orElseThrow(() -> new NotFoundException(usuarioNoEncontrado));
        String salida;
        userSelect.setActivo(false);
        userRep.save(userSelect);
        salida= ("Usuario con id "+id+" borrado correctamente");
        return salida;
    }

    /**
     * Recupera los roles asignados a un usuario.
     * Si el usuario no existe, devuelve una lista vacía.
     *
     * @param idUser identificador del usuario del que se desean consultar los roles
     * @return lista de roles asignados al usuario; vacía si el usuario no existe
     */
    @Override
    public List<Rol>rolesUser(Long idUser){
        List<Rol> roles;
        User encontrado=userRep.findById(idUser).orElseThrow(() -> new NotFoundException(usuarioNoEncontrado));
        roles = encontrado.getRoles();
        return roles;
    }

    /**
     * Añade un rol existente a la colección de roles de un usuario.
     *
     * @param idUser identificador del usuario al que se le asignará el rol
     * @param idRol DTO que contiene el identificador del rol a asignar
     * @return mensaje indicando si la asignación se realizó correctamente o si no se encontró usuario/rol
     */
    @Override
    public String addRolUser(Long idUser, RolPostUser idRol) throws DuplicateException {
        String salida;
        User encontrado=userRep.findById(idUser).orElseThrow(() -> new NotFoundException(usuarioNoEncontrado));
        Rol rolN=rolRep.findById(idRol.getIdRol()).orElseThrow(() -> new NotFoundException(rolNoEncontrado));
        List<Rol> roles = encontrado.getRoles();
        for (Rol r:roles){
            if(Objects.equals(r.getIdRol(), idRol.getIdRol())){
                throw new DuplicateException("Error: El usuario ya tiene ese rol.");
            }
        }
        roles.add(rolN);
        encontrado.setRoles(roles);
        userRep.save(encontrado);
        salida= "Rol con id "+rolN.getIdRol()+" añdadido correctamente a usuario con id "+idUser;
        return salida;
    }

    /**
     * Elimina la asociación de un rol con un usuario.
     * Si el usuario y el rol existen y el rol está asignado al usuario, se elimina y se persiste el cambio.
     *
     * @param idUser identificador del usuario al que se le desea eliminar el rol
     * @param idRol identificador del rol que se desea quitar del usuario
     * @return mensaje indicando el resultado: usuario/rol no encontrado, rol no asignado o éxito en la eliminación
     */
    @Override
    public String deleteRolUser(Long idUser, Long idRol) {

        User usuario = userRep.findById(idUser).orElseThrow(() -> new NotFoundException("El usuario introducido no existe en el sistema."));

        Rol rolN = rolRep.findById(idRol).orElseThrow(() -> new NotFoundException("El rol introducido no existe en el sistema."));

        //acción directa: removeif hace el bucle y el borrado de forma segura, guarda el resultado en un boolean
        boolean rolBorrado = usuario.getRoles().removeIf(rol ->
                Objects.equals(rol.getIdRol(), rolN.getIdRol())
        );

        if (rolBorrado) {
            userRep.save(usuario);
            return "Rol con id "+idRol+" eliminado correctamente del usuario con id "+idUser;
        }
        throw new NotFoundException("Error: El usuario no tenía asignado ese rol.");
    }
}

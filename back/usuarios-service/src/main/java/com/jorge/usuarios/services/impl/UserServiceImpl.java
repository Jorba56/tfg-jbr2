package com.jorge.usuarios.services.impl;

import com.jorge.usuarios.entity.Item;
import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.repository.ItemRepository;
import com.jorge.usuarios.repository.RolRepository;
import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.dto.RolPostUser;
import com.jorge.usuarios.dto.UserAddDTO;
import com.jorge.usuarios.dto.UsersAllDTO;
import com.jorge.usuarios.repository.UserRepository;

import com.jorge.usuarios.exceptions.*;
import com.jorge.usuarios.mapping.UserMapper;
import com.jorge.usuarios.services.UserService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import java.util.*;

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
    private final ItemRepository itemRepository;
    private final EmailServiceImpl emailService;


    /**
     * Crea una nueva instancia del servicio de usuarios inyectando sus dependencias.
     * Inicializa el repositorio de usuarios, el mapper de usuarios y el repositorio de roles
     * necesarios para las operaciones del servicio.
     *
     * @param userRep repositorio para realizar operaciones de persistencia sobre usuarios
     * @param userMap mapper encargado de transformar entidades de usuario y sus DTOs
     * @param rolRep repositorio para la gestión de la persistencia de roles asociados a usuarios
     */
    public UserServiceImpl(UserRepository userRep,EmailServiceImpl emailService, UserMapper userMap, RolRepository rolRep, PasswordEncoder passwordEncoder, com.jorge.usuarios.repository.ItemRepository itemRepository) {
        this.userRep = userRep;
        this.emailService=emailService;
        this.userMap = userMap;
        this.rolRep = rolRep;
        this.passwordEncoder = passwordEncoder;
        this.itemRepository = itemRepository; // NUEVO
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
    public UsersAllDTO buscarPorId(Long id) {
        User usuarioEncontrado = userRep.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado con ID: " + id));

        if (!usuarioEncontrado.getActivo()) {
            throw new NotFoundException("El usuario con ID " + id + " está desactivado y no se puede mostrar.");
        }
        return userMap.mappingADTO(usuarioEncontrado);
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

        Rol rolN=rolRep.findByName(("USUARIO")).orElseThrow(() -> new NotFoundException(rolNoEncontrado));
        roles.add(rolN);
        usuario2.setRoles(roles);
        userRep.save(usuario2);
        System.out.println("DEBUG: Intentando enviar correo a: " + usuario.getEmailUsuario());
        System.out.println("DEBUG: Nombre del piloto: " + usuario.getNombreUsuario());

        try {
            emailService.enviarCorreoBienvenida(usuario.getEmailUsuario(), usuario.getNombreUsuario());
            System.out.println("DEBUG: Llamada al servicio de email completada.");
        } catch (Exception e) {
            System.out.println("DEBUG: EXPLOTÓ EL ENVÍO: " + e.getMessage());
            e.printStackTrace();
        }
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

        // 1. Solo actualizamos el nombre si nos envían uno nuevo
        if (usuario.getNombreUsuario() != null && !usuario.getNombreUsuario().trim().isEmpty()) {
            userUpdate.setNombreUsuario(usuario.getNombreUsuario());
        }

        // 2. Solo actualizamos el apellido si nos envían uno
        if (usuario.getApellidoUsuario() != null && !usuario.getApellidoUsuario().trim().isEmpty()) {
            userUpdate.setApellidoUsuario(usuario.getApellidoUsuario());
        }

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

    @Override
    public List<Item> listarTienda() {
        return itemRepository.findAll();
    }

    @Override
    @Transactional
    public Map<String, Object> comprarItem(String emailUsuario, Long itemId) throws BadRequestException {
        // 1. Buscamos al usuario (En tu repo devuelve User, no Optional)
        User user = userRep.findUserByEmailUsuario(emailUsuario);
        if (user == null) {
            throw new BadRequestException("Usuario no encontrado");
        }

        // 2. Buscamos el objeto
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BadRequestException("El objeto no existe"));

        // 3. Validar si ya lo tiene
        if (user.getInventario().contains(item)) {
            throw new BadRequestException("Ya posees este objeto en tu inventario");
        }

        // 4. Validar saldo
        if (user.getCreditos() < item.getPrecio()) {
            throw new BadRequestException("Créditos insuficientes");
        }

        // 5. Transacción
        user.setCreditos(user.getCreditos() - item.getPrecio());
        user.getInventario().add(item);

        userRep.save(user); // Corregido: es userRep, no userRepository

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("mensaje", "Compra realizada con éxito");
        response.put("nuevoSaldo", user.getCreditos());

        return response;
    }

    @Override
    @Transactional
    public Map<String, Object> sumarCreditosAdmin(Long idUsuario, int cantidad) throws BadRequestException {
        User user = userRep.findById(idUsuario)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado con ID: " + idUsuario));

        // Sumamos los créditos a los que ya tenía
        user.setCreditos(user.getCreditos() + cantidad);
        userRep.save(user);

        // Devolvemos un JSON de confirmación
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("mensaje", "Se han sumado " + cantidad + " créditos. Saldo actual: " + user.getCreditos());
        response.put("nuevos_creditos", user.getCreditos());
        return response;
    }

    @Override
    public List<UsersAllDTO> obtenerTodosLosUsuarios(String sortBy, String sortDir) {
        // traducimos el campo de la url al nombre real de la base de datos
        String campoEntidad = traducirCampoSortUsuario(sortBy);

        // creamos la ordenación (ascendente o descendente)
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(campoEntidad).ascending()
                : Sort.by(campoEntidad).descending();

        // jparepository ya tiene un findall(sort) incorporado
        List<User> usuarios = userRep.findByActivoTrue(sort);
        List <UsersAllDTO> usuariosMappeados= new ArrayList<>();

        for (User usuario : usuarios) {
            usuariosMappeados.add(userMap.mappingADTO(usuario));
        }
        return usuariosMappeados;
    }

    @Override
    public Page<UsersAllDTO> obtenerTodosLosUsuariosPaginados(int page, int size, String sortBy, String sortDir) {
        String campoEntidad = traducirCampoSortUsuario(sortBy); // Usamos switch traductor

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(campoEntidad).ascending()
                : Sort.by(campoEntidad).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> paginaUsuarios = userRep.findByActivoTrue(pageable);

        // page tiene un .map() integrado que funciona como un for-each
        return paginaUsuarios.map(userMap::mappingADTO);
    }

    private String traducirCampoSortUsuario(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "nombre", "nombreusuario" -> "nombreUsuario";
            case "apellido", "apellidousuario" -> "apellidoUsuario";
            case "correo", "email", "emailusuario" -> "emailUsuario";
            default -> "idUser"; // O el nombre que tenga tu clave primaria en la entidad User
        };
    }
    @Override
    public UsersAllDTO buscarPorEmail(String correo) {
        User usuario= userRep.findUserByEmailUsuario(correo);
        return userMap.mappingADTO(usuario);
    }

    @Override
    public User buscarPorEmailTodo(String correo) {
        return userRep.findUserByEmailUsuario(correo);
    }

    @Transactional
    public void sumarCreditosPartida(String email, Integer creditosGanados) {
        // 1. Buscamos al piloto
        User usuario = userRep.findUserByEmailUsuario(email);

        // 2. Le sumamos el premio
        int totalCreditos = usuario.getCreditos() + creditosGanados;
        usuario.setCreditos(totalCreditos);

        // 3. Apretamos los tornillos (Guardar en BBDD)
        userRep.save(usuario);
    }

    /**
     * Obtiene los datos del perfil de un usuario logueado.
     * Devuelve un mapa con la información esencial necesaria para el frontend (nombre, créditos, avatar e inventario).
     *
     * @param emailLogueado correo electrónico del usuario extraído del contexto de seguridad
     * @return mapa con los datos del perfil del usuario listos para ser serializados a JSON
     * @throws NotFoundException si el usuario no es encontrado en la base de datos
     */
    @Override
    public Map<String, Object> getPropioPerfil(String emailLogueado) {
        User usuario = userRep.findUserByEmailUsuario(emailLogueado);

        if (usuario == null) {
            throw new NotFoundException("Usuario no encontrado en el sistema.");
        }

        Map<String, Object> perfil = new HashMap<>();
        perfil.put("id", usuario.getIdUser());
        perfil.put("username", usuario.getNombreUsuario());
        perfil.put("apellido", usuario.getApellidoUsuario());
        perfil.put("creditos", usuario.getCreditos());
        perfil.put("avatar", usuario.getAvatarConfig() != null ? usuario.getAvatarConfig() : usuario.getNombreUsuario());

        boolean isAdmin = false;
        if (usuario.getRoles() != null) {
            for (Rol rol : usuario.getRoles()) {
                // OJO: Si tu getter del nombre del rol no es getNombreRol(), cámbialo por getNombre() o getName()
                String nombreRol = rol.getName();
                if (nombreRol != null && nombreRol.toUpperCase().contains("ADMIN")) {
                    isAdmin = true;
                    break;
                }
            }
        }
        // Metemos el pase VIP en el paquete
        perfil.put("isAdmin", isAdmin);

        // 🛠️ EL CINTURÓN DE SEGURIDAD: Mapeamos el inventario a mano para que JS nunca se confunda
        List<Map<String, Object>> inventarioLimpio = new ArrayList<>();
        if (usuario.getInventario() != null) {
            for (Item item : usuario.getInventario()) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("id_item", item.getIdItem());
                itemData.put("nombre", item.getNombre());
                itemData.put("descripcion", item.getDescripcion());
                inventarioLimpio.add(itemData);
            }
        }
        perfil.put("inventario", inventarioLimpio);

        return perfil;
    }

    @Transactional
    @Override
    public Map<String, Object> actualizarPerfil(String emailLogueado, Map<String, String> payload) {
        User usuario = userRep.findUserByEmailUsuario(emailLogueado);

        if (usuario == null) {
            throw new NotFoundException("Usuario no encontrado en el sistema.");
        }

        // 1. Actualizamos el nombre si viene en el paquete
        if (payload.containsKey("nombre_usuario") && !payload.get("nombre_usuario").trim().isEmpty()) {
            usuario.setNombreUsuario(payload.get("nombre_usuario"));
        }

        // 2. Actualizamos la contraseña si viene en el paquete
        if (payload.containsKey("contrasenha_usuario") && !payload.get("contrasenha_usuario").trim().isEmpty()) {
            // Nota: Si usas PasswordEncoder en tu proyecto, deberías envolver esto en un .encode()
            // ej: usuario.setContrasenhaUsuario(passwordEncoder.encode(payload.get("contrasenha_usuario")));
            usuario.setContrasenhaUsuario(passwordEncoder.encode(payload.get("contrasenha_usuario")));
        }

        // 3. Apretamos tuercas en BBDD
        userRep.save(usuario);

        return Map.of("mensaje", "Perfil actualizado con éxito");
    }

    /**
     * Actualiza la cadena de configuración del avatar (DiceBear) de un usuario en la base de datos.
     *
     * @param emailLogueado correo electrónico del usuario que solicita el cambio
     * @param avatarConfig cadena de configuración generada por el frontend (ej. "Nombre&top=hat")
     * @return mapa con el mensaje de confirmación de la operación
     * @throws NotFoundException si el usuario no es encontrado en la base de datos
     */
    @Transactional
    @Override
    public Map<String, Object> actualizarAvatar(String emailLogueado, String avatarConfig) {
        User usuario = userRep.findUserByEmailUsuario(emailLogueado);

        if (usuario == null) {
            throw new NotFoundException("Usuario no encontrado en el sistema.");
        }

        // Guardamos la "receta" que viene del frontend
        usuario.setAvatarConfig(avatarConfig);
        userRep.save(usuario);

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "¡Avatar guardado con éxito!");

        return response;
    }

    public void guardarEstadisticasPartida(String email, Map<String, Object> datosPartida) {
        // Buscamos por el email completo que nos da Spring Security
        User user = userRep.findUserByEmailUsuario(email);
        if (user==null){throw new NotFoundException("Usuario no encontrado.");}

        int creditosNuevos = ((Number) datosPartida.getOrDefault("creditos", 0)).intValue();
        user.setCreditos(user.getCreditos() + creditosNuevos);

        String modo = (String) datosPartida.getOrDefault("modo", "");

        if ("ROSCO".equals(modo)) {
            int totalPalabras = ((Number) datosPartida.getOrDefault("totalPalabras", 25)).intValue();
            int puntuacionFinal = ((Number) datosPartida.getOrDefault("puntuacion", 0)).intValue();
            user.setRoscoPalabrasJugadas(user.getRoscoPalabrasJugadas() + totalPalabras);

            if (puntuacionFinal > user.getMejorPuntuacionRosco()) {
                user.setMejorPuntuacionRosco(puntuacionFinal);
            }
        }
        else if ("CONTRARRELOJ".equals(modo)) {
            int frasesOk = ((Number) datosPartida.getOrDefault("aciertos", 0)).intValue();
            int frasesTotal = ((Number) datosPartida.getOrDefault("totalPalabras", 0)).intValue();
            int puntuacionFinal = ((Number) datosPartida.getOrDefault("puntuacion", 0)).intValue();

            user.setFrasesAcertadas(user.getFrasesAcertadas() + frasesOk);
            user.setFrasesJugadas(user.getFrasesJugadas() + frasesTotal);

            if (puntuacionFinal > user.getMejorPuntuacionContrarreloj()) {
                user.setMejorPuntuacionContrarreloj(puntuacionFinal);
            }
        }
        userRep.save(user);
    }

    // --- OBTENER ESTADÍSTICAS POR ID ---
    // 💡 Usamos el ID para evitar problemas con puntos o caracteres especiales en el email
    public Map<String, Object> obtenerEstadisticasPublicas(Long id) {
        User u = userRep.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado."));

        Map<String, Object> stats = new HashMap<>();

        // extraer lo que hay antes del @
        String email = u.getEmailUsuario();
        String alias = email.substring(0, email.indexOf("@"));

        stats.put("username", alias);
        stats.put("avatar", u.getAvatarConfig());
        stats.put("creditos", u.getCreditos());
        stats.put("mejorRosco", u.getMejorPuntuacionRosco());
        stats.put("mejorContrarreloj", u.getMejorPuntuacionContrarreloj());

        double pctFrases = u.getFrasesJugadas() == 0 ? 0 :
                (u.getFrasesAcertadas() * 100.0) / u.getFrasesJugadas();

        stats.put("pctFrases", Math.round(pctFrases));
        return stats;
    }

    // --- RANKING GLOBAL ---
    public List<Map<String, Object>> obtenerRankingGlobal() {
        List<User> topUsuarios = userRep.findTop10ByOrderByCreditosDesc();
        List<Map<String, Object>> rankingSeguro = new ArrayList<>();

        for (User u : topUsuarios) {
            Map<String, Object> piloto = new HashMap<>();

            // aplicamos la misma lógica de subcadena
            String email = u.getEmailUsuario();
            String alias = email.substring(0, email.indexOf("@"));

            piloto.put("id", u.getIdUser()); // el id es vital para el clic del frontend
            piloto.put("username", alias);
            piloto.put("creditos", u.getCreditos());
            piloto.put("avatar", u.getAvatarConfig());
            rankingSeguro.add(piloto);
        }
        return rankingSeguro;
    }
}

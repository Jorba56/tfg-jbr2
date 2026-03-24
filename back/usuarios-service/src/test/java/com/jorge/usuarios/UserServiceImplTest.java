package com.jorge.usuarios;

import com.jorge.usuarios.dto.*;
import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.exceptions.BadRequestException;
import com.jorge.usuarios.exceptions.DuplicateException;
import com.jorge.usuarios.exceptions.NotFoundException;
import com.jorge.usuarios.mapping.UserMapper;
import com.jorge.usuarios.repository.RolRepository;
import com.jorge.usuarios.repository.UserRepository;
import com.jorge.usuarios.services.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)

class UserServiceImplTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RolRepository rolRep;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMap;

    @InjectMocks
    private UserServiceImpl userServiceImpl;

    @Test
    void getAllUsers() {
        User usuario = new User();
        UsersAllDTO dtoFalso = new UsersAllDTO();

        given(userRepository.findUsersByActivoIs(true)).willReturn(List.of(usuario));
        given(userMap.mappingADTO(usuario)).willReturn(dtoFalso);

        List<UsersAllDTO> rolList = userServiceImpl.listarUsuarios();

        assertFalse(rolList.isEmpty());
        assertEquals(1, rolList.size());
        verify(userMap).mappingADTO(usuario);
        verify(userRepository).findUsersByActivoIs(true);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void getAllUsersVacio() {

        given(userRepository.findUsersByActivoIs(true)).willReturn(List.of());


        List<UsersAllDTO> rolList = userServiceImpl.listarUsuarios();

        assertTrue(rolList.isEmpty());
        verify(userRepository).findUsersByActivoIs(true);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void getUserId() {
        Long id = 1L;
        User usuario = new User();
        usuario.setIdUser(id);
        usuario.setNombreUsuario("Jorge");

        UserIdDTo dtoEsperado = new UserIdDTo();
        dtoEsperado.setNombreUsuario("Jorge");

        given(userRepository.findById(1L)).willReturn(Optional.of(usuario));
        given(userMap.userToIdDTO(usuario)).willReturn(dtoEsperado);

        //when
        UserIdDTo userFind = userServiceImpl.buscarPorId(1L);

        assertNotNull(userFind);
        assertEquals(("Jorge"), userFind.getNombreUsuario());
    }

    @Test
    void getUserIdNull() {

        given(userRepository.findById(99L)).willReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> userServiceImpl.buscarPorId(99L)
        );

        assertEquals("Usuario no encontrado con ID: "+99L, ex.getMessage());

        verify(userRepository).findById(99L);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void getUserId_Inactivo() {
        User usuario = new User();
        usuario.setIdUser(1L);
        usuario.setActivo(false); // Simulamos que está inactivo

        given(userRepository.findById(1L)).willReturn(Optional.of(usuario));

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> userServiceImpl.buscarPorId(1L)
        );

        assertEquals("El usuario con ID 1 está desactivado y no se puede mostrar.", ex.getMessage());
    }

    @Test
    void addUser() throws DuplicateException {
        // preparar dto de entrada (con los datos ya puestos desde el principio)
        UserAddDTO userdto = new UserAddDTO();
        userdto.setEmailUsuario("mbappe09@gmail.com");
        userdto.setContrasenhaUsuario("123abc");
        userdto.setNombreUsuario("Kylian");

        // preparar el usuario que creará el mapper
        User usuarioU = new User();
        usuarioU.setNombreUsuario("Kylian");

        // preparar el rol que sacaremos de la bd
        Rol alumno = new Rol();
        alumno.setIdRol(1L);
        alumno.setName("alumno");

        // preparar el dto final que se devuelve al cliente
        UsersAllDTO userdto2 = new UsersAllDTO();
        userdto2.setNombreUsuario("Kylian");

        // educar a los mocks en el orden correcto
        given(userRepository.findUserByEmailUsuario(userdto.getEmailUsuario())).willReturn(null);
        given(userMap.userAddDTO(userdto)).willReturn(usuarioU);
        given(passwordEncoder.encode(userdto.getContrasenhaUsuario())).willReturn("claveEncriptada");
        given(rolRep.findByName("alumno")).willReturn(Optional.of(alumno));
        given(userRepository.save(usuarioU)).willReturn(usuarioU);
        given(userMap.mappingADTO(usuarioU)).willReturn(userdto2);

        // ejecutamos el metodo del servicio
        UsersAllDTO respuesta = userServiceImpl.addUsuario(userdto);

        // 7. Comprobaciones (Aserciones)
        assertNotNull(respuesta);
        assertEquals("Kylian", respuesta.getNombreUsuario());

        // comprobamos que las líneas que te faltaban hacen su trabajo
        assertFalse(usuarioU.getRoles().isEmpty());
        assertEquals("alumno", usuarioU.getRoles().getFirst().getName());
        assertEquals("claveEncriptada", usuarioU.getContrasenhaUsuario());

        // verificamos que se llamó al guardado de la BD
        verify(userRepository).save(usuarioU);
    }

    @Test
    void addUser_EmailDuplicado() {
        UserAddDTO userdto = new UserAddDTO();
        userdto.setEmailUsuario("duplicado@gmail.com");

        // Simulamos que el repositorio ya encuentra a alguien con ese correo
        given(userRepository.findUserByEmailUsuario("duplicado@gmail.com")).willReturn(new User());

        DuplicateException ex = assertThrows(
                DuplicateException.class,
                () -> userServiceImpl.addUsuario(userdto)
        );

        assertEquals("El correo electrónico ya está en uso.", ex.getMessage());
        verify(userRepository, never()).save(any()); // Comprobamos que no se guardó
    }

    @Test
    void addUser_RolAlumnoNoExiste() {
        UserAddDTO userdto = new UserAddDTO();
        userdto.setEmailUsuario("nuevo@gmail.com");
        userdto.setContrasenhaUsuario("123");

        User usuarioMapeado = new User();

        given(userRepository.findUserByEmailUsuario(anyString())).willReturn(null);
        given(userMap.userAddDTO(userdto)).willReturn(usuarioMapeado);
        given(passwordEncoder.encode(anyString())).willReturn("encriptada");

        // Simulamos que el rol "alumno" no existe en la BD
        given(rolRep.findByName("alumno")).willReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> userServiceImpl.addUsuario(userdto)
        );

        assertEquals("El rol introducido no existe en el sistema.", ex.getMessage());
    }

    @Test
    void deleteUser(){
        User user=new User();
        user.setIdUser(6L);
        user.setActivo(true);// no necesito más

        given(userRepository.findById(6L)).willReturn(Optional.of(user));
        //when
        String borrado = userServiceImpl.desactivarUsuario(6L);

        //asserts
        assertFalse(user.getActivo());
        assertNotNull(borrado);
        assertEquals("Usuario con id "+6L+" borrado correctamente", borrado);
        verify(userRepository).save(user);
    }

    @Test
    void deleteUserNull() {
        // Obligamos a Mockito a devolver vacío
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // WHEN
        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> userServiceImpl.desactivarUsuario(99L)
        );

        assertEquals("El usuario introducido no existe en el sistema.", ex.getMessage());
    }

    @Test
    void rolesUser() {
        User user = new User();
        user.setIdUser(6L);
        user.setActivo(true);

        Rol rolFalso = new Rol();
        rolFalso.setIdRol(1L);
        rolFalso.setName("admin");
        user.setRoles(List.of(rolFalso)); // Añadimos el rol al usuario

        given(userRepository.findById(6L)).willReturn(Optional.of(user));

        List<Rol> roles = userServiceImpl.rolesUser(user.getIdUser());

        assertNotNull(roles);
        assertFalse(roles.isEmpty()); // Comprobamos que la lista NO viene vacía
        assertEquals(1, roles.size()); // Comprobamos que trae exactamente 1 rol
        assertEquals("admin", roles.getFirst().getName()); // Comprobamos que es el rol correcto

        verify(userRepository).findById(6L);
    }


    @Test
    void deleteRolUser() {
        //Preparamos un rol
        Rol rolAEliminar = new Rol();
        rolAEliminar.setIdRol(2L);

        //Preparamos un usuario que YA TIENE ese rol asignado
        User usuario = new User();
        usuario.setIdUser(1L);
        List<Rol> rolesDelUsuario = new ArrayList<>();
        rolesDelUsuario.add(rolAEliminar);
        usuario.setRoles(rolesDelUsuario);

        // Educamos a los mocks
        given(userRepository.findById(1L)).willReturn(Optional.of(usuario));
        given(rolRep.findById(2L)).willReturn(Optional.of(rolAEliminar));

        // Ejecutamos el servicio
        String resultado = userServiceImpl.deleteRolUser(1L, 2L);

        //Comprobamos el texto
        assertNotNull(resultado);
        assertEquals("Rol con id "+2L+" eliminado correctamente del usuario con id "+1L, resultado);

        //Comprobamos que la lista del usuario ahora está vacía (se ha borrado)
        assertTrue(usuario.getRoles().isEmpty());

        //Comprobamos que los cambios se guardaron
        verify(userRepository).save(usuario);
        verify(userRepository).findById(1L);
        verify(rolRep).findById(2L);
    }

    @Test
    void rolesUser_UserNotFound() {
        //El usuario 99 no existe
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // WHEN

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> userServiceImpl.rolesUser(99L)
        );

        assertEquals("El usuario introducido no existe en el sistema.", ex.getMessage());
        // THEN: Devuelve una lista vacía y no da error
        verify(userRepository).findById(99L);
    }

    @Test
    void addRolUser_UserNotFound() {
        RolPostUser rolPost = new RolPostUser();
        rolPost.setIdRol(2L);

        //El usuario 99 no existe
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> userServiceImpl.addRolUser(99L, rolPost)
        );

        // THEN: Comprobamos el mensaje de error
        assertEquals("El usuario introducido no existe en el sistema.", ex.getMessage());

    }

    @Test
    void addRolUser_RolNotFound() {
        // GIVEN
        Long idUser = 1L;
        RolPostUser rolDto = new RolPostUser();
        rolDto.setIdRol(99L);

        User usuarioReal = new User();
        usuarioReal.setIdUser(1L);

        // Educamos a los mocks: El usuario SÍ existe, pero el rol NO existe
        given(userRepository.findById(idUser)).willReturn(Optional.of(usuarioReal));
        given(rolRep.findById(rolDto.getIdRol())).willReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> userServiceImpl.addRolUser(1L, rolDto)
        );

        // Verifica el mensaje de la excepción
        assertEquals("El rol introducido no existe en el sistema.", ex.getMessage());
        verify(userRepository , never()).save(any(User.class));
    }

    @Test
    void deleteRolUser_UserNotFound() {
        // GIVEN: El usuario no existe
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> userServiceImpl.deleteRolUser(99L, 2L)
        );

        // Verifica el mensaje de la excepción
        assertEquals("El usuario introducido no existe en el sistema.", ex.getMessage());
        verifyNoInteractions(rolRep); // No llega a buscar el rol
    }

    @Test
    void deleteRolUser_RolNotFound() {
        //El usuario sí existe
        User usuario = new User();
        usuario.setIdUser(1L);

        given(userRepository.findById(1L)).willReturn(Optional.of(usuario));
        // Pero el rol que intentamos borrar no existe en la BD
        given(rolRep.findById(99L)).willReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> userServiceImpl.deleteRolUser(1L, 99L)
        );

        // Verifica el mensaje de la excepción
        assertEquals("El rol introducido no existe en el sistema.", ex.getMessage());
        // Comprobamos que nunca hace "save" por error
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteRolUser_RolNotAssigned() {
        // GIVEN: El usuario existe y tiene un rol, pero es DISTINTO al que queremos borrar
        User usuario = new User();
        usuario.setIdUser(1L);

        Rol rolDistinto = new Rol();
        rolDistinto.setIdRol(8L); // Diferente a 2L

        List<Rol> roles = new ArrayList<>();
        roles.add(rolDistinto);
        usuario.setRoles(roles);

        // El rol que queremos borrar
        Rol rolAEliminar = new Rol();
        rolAEliminar.setIdRol(2L);

        given(userRepository.findById(1L)).willReturn(Optional.of(usuario));
        given(rolRep.findById(2L)).willReturn(Optional.of(rolAEliminar));

        // WHEN
        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> userServiceImpl.deleteRolUser(1L, 2L)
        );

        // THEN
        assertEquals("Error: El usuario no tenía asignado ese rol.", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void addRolUser_Exito_ListaVacia() throws DuplicateException {
        // ESCENARIO 1: Usuario totalmente nuevo sin roles (El bucle FOR se salta)
        User usuario = new User();
        usuario.setIdUser(1L);
        usuario.setRoles(new ArrayList<>()); // Lista vacía

        Rol rolNuevo = new Rol();
        rolNuevo.setIdRol(2L);
        rolNuevo.setName("rol");

        RolPostUser rolPost = new RolPostUser();
        rolPost.setIdRol(2L);

        given(userRepository.findById(1L)).willReturn(Optional.of(usuario));
        given(rolRep.findById(2L)).willReturn(Optional.of(rolNuevo));

        String resultado = userServiceImpl.addRolUser(1L, rolPost);

        assertEquals("Rol con id 2 añdadido correctamente a usuario con id 1", resultado);
        assertEquals(1, usuario.getRoles().size());
    }

    @Test
    void addRolUser_Exito_ConRolDistinto() throws DuplicateException {
        // ESCENARIO 2: Usuario ya tiene un rol, pero es distinto (El IF da FALSE)
        User usuario = new User();
        usuario.setIdUser(1L);

        Rol rolPrevio = new Rol();
        rolPrevio.setIdRol(8L); // ID distinto (8 vs 2)

        List<Rol> listaRoles = new ArrayList<>();
        listaRoles.add(rolPrevio);
        usuario.setRoles(listaRoles);

        Rol rolNuevo = new Rol();
        rolNuevo.setIdRol(2L);

        RolPostUser rolPost = new RolPostUser();
        rolPost.setIdRol(2L);

        given(userRepository.findById(1L)).willReturn(Optional.of(usuario));
        given(rolRep.findById(2L)).willReturn(Optional.of(rolNuevo));

        String resultado = userServiceImpl.addRolUser(1L, rolPost);

        assertEquals("Rol con id 2 añdadido correctamente a usuario con id 1", resultado);
        assertEquals(2, usuario.getRoles().size()); // Ahora tiene 2 roles
    }

    @Test
    void addRolUser_Error_RolYaAsignado() {
        // ESCENARIO 3: El usuario ya tiene ese mismo rol exacto (El IF da TRUE)
        User usuario = new User();
        usuario.setIdUser(1L);

        Rol rolPrevio = new Rol();
        rolPrevio.setIdRol(2L); // ID idéntico (2 vs 2)

        List<Rol> rolesActuales = new ArrayList<>();
        rolesActuales.add(rolPrevio);
        usuario.setRoles(rolesActuales);

        Rol rolEnBaseDeDatos = new Rol();
        rolEnBaseDeDatos.setIdRol(2L);

        RolPostUser idRolEntrada = new RolPostUser();
        idRolEntrada.setIdRol(2L);

        given(userRepository.findById(1L)).willReturn(Optional.of(usuario));
        given(rolRep.findById(2L)).willReturn(Optional.of(rolEnBaseDeDatos));

        DuplicateException ex = assertThrows(
                DuplicateException.class,
                () -> userServiceImpl.addRolUser(1L, idRolEntrada)
        );

        assertEquals("Error: El usuario ya tiene ese rol.", ex.getMessage());
        verify(userRepository, never()).save(any(User.class)); // Verificamos que no guardó nada
    }

    @Test
    void actualizarUsuario_comoAdmin_exito() throws BadRequestException {
        // preparar usuario en bd
        User usuarioBd = new User();
        usuarioBd.setActivo(true);
        usuarioBd.setEmailUsuario("alumno@gmail.com");

        // preparar datos a cambiar
        User usuarioNuevosDatos = new User();
        usuarioNuevosDatos.setNombreUsuario("Paco");
        usuarioNuevosDatos.setActivo(true);

        // mock de autenticacion como admin
        Authentication authMock = mock(Authentication.class);
        given(authMock.getName()).willReturn("admin@gmail.com");
        doReturn(List.of(new SimpleGrantedAuthority("ADMIN"))).when(authMock).getAuthorities();

        // simular busqueda
        given(userRepository.findById(1L)).willReturn(Optional.of(usuarioBd));

        // ejecutar
        String resultado = userServiceImpl.actualizarUsuario(1L, usuarioNuevosDatos, authMock);

        // comprobar
        assertEquals("Usuario con id 1 editado correctamente", resultado);
        verify(userRepository).save(usuarioBd); // verifica que se llamo a guardar
    }

    @Test
    void actualizarUsuario_mismoUsuario_exito() throws BadRequestException {
        // preparar usuario en bd
        User usuarioBd = new User();
        usuarioBd.setActivo(true);
        usuarioBd.setEmailUsuario("paco@gmail.com");

        // preparar datos a cambiar (cambia contrasena)
        User usuarioNuevosDatos = new User();
        usuarioNuevosDatos.setEmailUsuario("paco@gmail.com");
        usuarioNuevosDatos.setContrasenhaUsuario("1234");
        usuarioNuevosDatos.setActivo(true);

        // mock de autenticacion como usuario normal (coincide el email)
        Authentication authMock = mock(Authentication.class);
        given(authMock.getName()).willReturn("paco@gmail.com");
        doReturn(List.of(new SimpleGrantedAuthority("ALUMNO"))).when(authMock).getAuthorities();

        // simular bd y encriptacion
        given(userRepository.findById(1L)).willReturn(Optional.of(usuarioBd));
        given(passwordEncoder.encode("1234")).willReturn("hash1234");

        // ejecutar
        String resultado = userServiceImpl.actualizarUsuario(1L, usuarioNuevosDatos, authMock);

        // comprobar
        assertEquals("Usuario con id 1 editado correctamente", resultado);
        assertEquals("hash1234", usuarioBd.getContrasenhaUsuario());
    }

    @Test
    void actualizarUsuario_distintoUsuario_lanzaExcepcion() {
        // preparar usuario en bd
        User usuarioBd = new User();
        usuarioBd.setActivo(true);
        usuarioBd.setEmailUsuario("paco@gmail.com"); // id 1 es de paco

        User usuarioNuevosDatos = new User();

        // mock de autenticacion como usuario distinto (laura)
        Authentication authMock = mock(Authentication.class);
        given(authMock.getName()).willReturn("laura@gmail.com");
        doReturn(List.of(new SimpleGrantedAuthority("ALUMNO"))).when(authMock).getAuthorities();

        given(userRepository.findById(1L)).willReturn(Optional.of(usuarioBd));

        // ejecutar y comprobar que explota con badrequest
        assertThrows(BadRequestException.class, () ->
                userServiceImpl.actualizarUsuario(1L, usuarioNuevosDatos, authMock)
        );
    }

    @Test
    void actualizarUsuario_adminIntentaCambiarPassword_lanzaExcepcion() {
        // preparar datos
        User usuarioBd = new User();
        usuarioBd.setActivo(true);
        usuarioBd.setEmailUsuario("alumno@gmail.com");

        User usuarioNuevosDatos = new User();
        usuarioNuevosDatos.setContrasenhaUsuario("12345"); // admin intentando cambiar pass

        // mock admin
        Authentication authMock = mock(Authentication.class);
        given(authMock.getName()).willReturn("admin@gmail.com");
        doReturn(List.of(new SimpleGrantedAuthority("ADMIN"))).when(authMock).getAuthorities();

        given(userRepository.findById(1L)).willReturn(Optional.of(usuarioBd));

        // comprobar excepcion
        assertThrows(BadRequestException.class, () ->
                userServiceImpl.actualizarUsuario(1L, usuarioNuevosDatos, authMock)
        );
    }

    @Test
    void actualizarUsuario_adminCambiaRoles_exito() throws BadRequestException {
        // preparar datos
        User usuarioBd = new User();
        usuarioBd.setActivo(true);
        usuarioBd.setEmailUsuario("alumno@gmail.com");

        User usuarioNuevosDatos = new User();
        usuarioNuevosDatos.setActivo(true);
        // admin asigna roles correctamente
        usuarioNuevosDatos.setRoles(List.of(new Rol()));

        // mock admin
        Authentication authMock = mock(Authentication.class);
        given(authMock.getName()).willReturn("admin@gmail.com");
        doReturn(List.of(new SimpleGrantedAuthority("ADMIN"))).when(authMock).getAuthorities();

        given(userRepository.findById(1L)).willReturn(Optional.of(usuarioBd));

        // ejecutar
        String resultado = userServiceImpl.actualizarUsuario(1L, usuarioNuevosDatos, authMock);

        // comprobar
        assertEquals("Usuario con id 1 editado correctamente", resultado);
    }

    @Test
    void actualizarUsuario_usuarioNormalIntentaCambiarRoles_lanzaExcepcion() {
        // preparar datos
        User usuarioBd = new User();
        usuarioBd.setActivo(true);
        usuarioBd.setEmailUsuario("paco@gmail.com");

        User usuarioNuevosDatos = new User();
        usuarioNuevosDatos.setEmailUsuario("paco@gmail.com");
        // usuario normal intentando hacerse admin
        usuarioNuevosDatos.setRoles(List.of(new Rol()));

        // mock usuario normal
        Authentication authMock = mock(Authentication.class);
        given(authMock.getName()).willReturn("paco@gmail.com");
        doReturn(List.of(new SimpleGrantedAuthority("ALUMNO"))).when(authMock).getAuthorities();

        given(userRepository.findById(1L)).willReturn(Optional.of(usuarioBd));

        // comprobar excepcion
        assertThrows(BadRequestException.class, () ->
                userServiceImpl.actualizarUsuario(1L, usuarioNuevosDatos, authMock)
        );
    }

    @Test
    void actualizarUsuario_usuarioNormalSinCambiarPassword_exito() throws BadRequestException {
        // preparar datos
        User usuarioBd = new User();
        usuarioBd.setActivo(true);
        usuarioBd.setEmailUsuario("paco@gmail.com");

        User usuarioNuevosDatos = new User();
        usuarioNuevosDatos.setEmailUsuario("paco@gmail.com");
        usuarioNuevosDatos.setContrasenhaUsuario(""); // vacio, no intenta cambiar pass

        // mock usuario normal
        Authentication authMock = mock(Authentication.class);
        given(authMock.getName()).willReturn("paco@gmail.com");
        doReturn(List.of(new SimpleGrantedAuthority("ALUMNO"))).when(authMock).getAuthorities();

        given(userRepository.findById(1L)).willReturn(Optional.of(usuarioBd));

        // ejecutar
        String resultado = userServiceImpl.actualizarUsuario(1L, usuarioNuevosDatos, authMock);

        // comprobar que NUNCA se llamo a encriptar porque la pass estaba vacia
        verify(passwordEncoder, never()).encode(anyString());
        assertEquals("Usuario con id 1 editado correctamente", resultado);
    }

    @Test
    void actualizarUsuario_usuarioNoExiste_lanzaExcepcion() {
        // mock de autenticacion basico
        Authentication authMock = mock(Authentication.class);
        User usuario = new User();

        // simular que la bd no encuentra al usuario
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // comprobar excepcion notfound
        assertThrows(NotFoundException.class, () ->
                userServiceImpl.actualizarUsuario(1L, usuario, authMock)
        );
    }

    @Test
    void actualizarUsuario_adminSinCambiarRoles_exito() throws BadRequestException {
        // preparar datos
        User usuarioBd = new User();
        usuarioBd.setActivo(true);
        usuarioBd.setEmailUsuario("alumno@gmail.com");

        User usuarioNuevosDatos = new User();
        usuarioNuevosDatos.setActivo(true);
        usuarioNuevosDatos.setRoles(null); // el admin manda null, no entra al if

        // mock admin
        Authentication authMock = mock(Authentication.class);
        given(authMock.getName()).willReturn("admin@gmail.com");
        doReturn(List.of(new SimpleGrantedAuthority("ADMIN"))).when(authMock).getAuthorities();

        given(userRepository.findById(1L)).willReturn(Optional.of(usuarioBd));

        // ejecutar
        String resultado = userServiceImpl.actualizarUsuario(1L, usuarioNuevosDatos, authMock);

        // comprobar
        assertEquals("Usuario con id 1 editado correctamente", resultado);
    }

    @Test
    void actualizarUsuario_usuarioNormalEnviaRolesVacios_exito() throws BadRequestException {
        // preparar datos
        User usuarioBd = new User();
        usuarioBd.setActivo(true);
        usuarioBd.setEmailUsuario("paco@gmail.com");

        User usuarioNuevosDatos = new User();
        usuarioNuevosDatos.setEmailUsuario("paco@gmail.com");
        usuarioNuevosDatos.setRoles(List.of()); // manda lista vacia, evalua a false el isEmpty()

        // mock usuario normal
        Authentication authMock = mock(Authentication.class);
        given(authMock.getName()).willReturn("paco@gmail.com");
        doReturn(List.of(new SimpleGrantedAuthority("ALUMNO"))).when(authMock).getAuthorities();

        given(userRepository.findById(1L)).willReturn(Optional.of(usuarioBd));

        // ejecutar
        String resultado = userServiceImpl.actualizarUsuario(1L, usuarioNuevosDatos, authMock);

        // comprobar
        assertEquals("Usuario con id 1 editado correctamente", resultado);
    }
}
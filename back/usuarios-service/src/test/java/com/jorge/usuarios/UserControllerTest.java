package com.jorge.usuarios;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.usuarios.controller.UserController;
import com.jorge.usuarios.dto.RolPostUser;
import com.jorge.usuarios.dto.UsersAllDTO;
import com.jorge.usuarios.dto.UserIdDTo;
import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.exceptions.DuplicateException;
import com.jorge.usuarios.mapping.UserMapper;
import com.jorge.usuarios.services.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)

class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // configura el controlador aislado (asegúrate de que tu variable inyectada se llame userController)
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
    }

    @Mock
    private UserMapper userMap;

    @Mock
    private UserServiceImpl userServiceImpl;

    @InjectMocks
    private UserController userController;

    @Test
     void getAllUsers(){
        UsersAllDTO usuario=new UsersAllDTO();
        usuario.setNombreUsuario("jorge");
        usuario.setApellidoUsuario("br");
        usuario.setEmailUsuario("jobr@gmail.com");

        given(userServiceImpl.listarUsuarios()).willReturn(List.of(usuario));

        //when
        List<UsersAllDTO> userList=userController.getAllUsers();

        assertFalse(userList.isEmpty());
        assertEquals((1), userList.size());
        verify(userServiceImpl).listarUsuarios();
        verifyNoMoreInteractions(userServiceImpl); // ver si no se ejecuta mas veces
    }

    @Test
     void getUserId(){
        UserIdDTo usuario=new UserIdDTo();
        usuario.setIdUser(1L);
        usuario.setNombreUsuario("jorge");
        usuario.setApellidoUsuario("br");
        usuario.setEmailUsuario("jobr@gmail.com");
        usuario.setActivo(true);

        given(userServiceImpl.buscarPorId(1L)).willReturn((usuario));

        //when
        UserIdDTo userFind = userController.getUserId(1L);

        assertNotNull(userFind);
        assertEquals(("jorge"), userFind.getNombreUsuario());
    }

    @Test
    void updateUser_DatosValidos_DeberiaDevolverMensajeExito() throws Exception {
        // datos de prueba
        Long userId = 1L;
        User usuarioModificado = new User();
        usuarioModificado.setNombreUsuario("PacoActualizado");
        usuarioModificado.setEmailUsuario("paco@gmail.com");

        // mock de autenticacion falsa
        Authentication authenticationMock = mock(Authentication.class);
        String mensajeEsperado = "Usuario con id 1 editado correctamente";

        // simular servicio
        given(userServiceImpl.actualizarUsuario(eq(userId), any(User.class), any(org.springframework.security.core.Authentication.class)))
                .willReturn(mensajeEsperado);

        // ejecutar peticion y verificar respuesta
        mockMvc.perform(put("/usuarios/{id}", userId)
                        .principal((java.security.Principal) authenticationMock)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usuarioModificado)))
                .andExpect(status().isOk())
                .andExpect(content().string(mensajeEsperado));
    }


    @Test
     void deleteUser(){
            User usuario=new User();
            usuario.setIdUser(1L);
            given(userServiceImpl.desactivarUsuario(usuario.getIdUser())).willReturn(("usuario borrado correctamente"));
            //when
            String borrado = userController.deleteUser(usuario.getIdUser());

            //asserts
            assertNotNull(borrado);
            assertEquals("usuario borrado correctamente", borrado);
    }

    @Test
    void getUserIdNull() {
        // el servicio no encuentra nada y devuelve null
        given(userServiceImpl.buscarPorId(99L)).willReturn(null);

        // when
        UserIdDTo userFind = userController.getUserId(99L);

        // then
        assertNull(userFind);
        verify(userServiceImpl).buscarPorId(99L);
    }

    @Test
    void rolesUser() {
        Rol rol = new Rol();
        rol.setIdRol(1L);
        rol.setName("admin");

        given(userServiceImpl.rolesUser(1L)).willReturn(List.of(rol));

        List<Rol> roles = userController.rolesUser(1L);

        assertFalse(roles.isEmpty());
        assertEquals("admin", roles.getFirst().getName());
        verify(userServiceImpl).rolesUser(1L);
    }

    @Test
    void userAddRol() throws DuplicateException {
        RolPostUser rolPost = new RolPostUser();
        rolPost.setIdRol(2L);

        given(userServiceImpl.addRolUser(1L, rolPost)).willReturn("rol añdadido a usuario");

        String resultado = userController.userAddRol(1L, rolPost);

        assertEquals("rol añdadido a usuario", resultado);
        verify(userServiceImpl).addRolUser(1L, rolPost);
    }

    @Test
    void deleteRolUser() {
        given(userServiceImpl.deleteRolUser(1L, 2L)).willReturn("Rol eliminado correctamente");

        String resultado = userController.deleteRolUser(2L, 1L);

        assertEquals("Rol eliminado correctamente", resultado);
    }
}
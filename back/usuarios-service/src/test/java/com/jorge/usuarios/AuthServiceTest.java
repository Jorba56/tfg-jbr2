package com.jorge.usuarios;


import com.jorge.usuarios.dto.LoginDTO;
import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.exceptions.BadRequestException;

import com.jorge.usuarios.exceptions.ConflictException;
import com.jorge.usuarios.mapping.UserMapper;
import com.jorge.usuarios.repository.UserRepository;
import com.jorge.usuarios.security.JwtUtil;

import com.jorge.usuarios.services.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)

class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMap;

    @Mock
    private JwtUtil jwtUtil;
    @InjectMocks
    private AuthServiceImpl authServiceImpl;

    @Test
    void login() throws ConflictException, BadRequestException {
        User usuario= new User();
        LoginDTO user= new LoginDTO();
        usuario.setIdUser(1L);
        usuario.setContrasenhaUsuario("1234");
        usuario.setNombreUsuario("luis");
        usuario.setApellidoUsuario("gomez");
        usuario.setEmailUsuario("luis@gmail.com");
        user.setEmailUsuario(usuario.getEmailUsuario());
        user.setContrasenhaUsuario(usuario.getContrasenhaUsuario());
        List<Rol> roles= new ArrayList<>();
        Rol rol= new Rol();
        rol.setName("alumno");
        rol.setIdRol(1L);
        rol.setActivo(true);
        roles.add(rol);
        usuario.setRoles(roles);
        List <String> rolesString= new ArrayList<>();
        rolesString.add(rol.getName());

        given(userRepository.findUserByEmailUsuario("luis@gmail.com")).willReturn(usuario);
        given(passwordEncoder.matches(user.getContrasenhaUsuario(), usuario.getContrasenhaUsuario())).willReturn(true);
        given(jwtUtil.generarToken("luis@gmail.com", rolesString)).willReturn("token123");

        Map<String, String> respuesta= authServiceImpl.login(user);

        assertNotNull(respuesta);
        assertEquals("token123", respuesta.get("token"));
    }

    @Test
    void loginCredencialesIncorrectasEmail(){
        LoginDTO user= new LoginDTO();
        user.setEmailUsuario("luis123@gmail.com");
        user.setContrasenhaUsuario("1234");
        given(userRepository.findUserByEmailUsuario("luis123@gmail.com")).willReturn(null);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authServiceImpl.login(user)
        );

        assertEquals("Credenciales de acceso incorrectas.", ex.getMessage());
    }

    @Test
    void loginCredencialesIncorrectasPasswd(){
        User usuario= new User();
        LoginDTO user= new LoginDTO();
        usuario.setIdUser(1L);
        usuario.setContrasenhaUsuario("1234");
        usuario.setEmailUsuario("luis@gmail.com");
        user.setEmailUsuario("luis@gmail.com");
        user.setContrasenhaUsuario("6567");

        given(userRepository.findUserByEmailUsuario("luis@gmail.com")).willReturn(usuario);
        given(passwordEncoder.matches(user.getContrasenhaUsuario(), usuario.getContrasenhaUsuario())).willReturn(false);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authServiceImpl.login(user)
        );

        assertEquals("Credenciales de acceso incorrectas.", ex.getMessage());
    }

    @Test
    void loginInactivo(){
        User usuario= new User();
        LoginDTO user= new LoginDTO();
        usuario.setIdUser(1L);
        usuario.setActivo(false);
        usuario.setContrasenhaUsuario("1234");
        usuario.setEmailUsuario("luis@gmail.com");
        user.setEmailUsuario("luis@gmail.com");
        user.setContrasenhaUsuario("1234");

        given(userRepository.findUserByEmailUsuario("luis@gmail.com")).willReturn(usuario);
        given(passwordEncoder.matches(user.getContrasenhaUsuario(), usuario.getContrasenhaUsuario())).willReturn(true);

        ConflictException ex = assertThrows(
                ConflictException.class,
                () -> authServiceImpl.login(user)
        );

        assertEquals("La cuenta de usuario se encuentra desactivada.", ex.getMessage());
    }

}

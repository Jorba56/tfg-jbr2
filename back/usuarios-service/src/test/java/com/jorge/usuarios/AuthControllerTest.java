package com.jorge.usuarios;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.usuarios.dto.LoginDTO;
import com.jorge.usuarios.dto.UserAddDTO;
import com.jorge.usuarios.dto.UsersAllDTO;
import com.jorge.usuarios.security.AuthController;
import com.jorge.usuarios.services.AuthService;
import com.jorge.usuarios.services.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;



    @Mock
    private AuthService authServiceImpl;

    @Mock
    private UserServiceImpl userServiceImpl;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // standaloneSetup para probar el controlador de forma aislada
        // sin levantar toda la configuración pesada de Spring Security
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void login_CredencialesCorrectas_DeberiaDevolverToken() throws Exception {

        LoginDTO loginDto = new LoginDTO();
        loginDto.setEmailUsuario("paco@gmail.com");
        loginDto.setContrasenhaUsuario("12345");

        Map<String, String> respuestaEsperada = new HashMap<>();
        respuestaEsperada.put("token", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIi...TokenFalso");


        given(authServiceImpl.login(any(LoginDTO.class))).willReturn(respuestaEsperada);

        //ejecución y verificación (when y then)
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk()) // Esperamos un HTTP 200
                .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiJ9.eyJzdWIi...TokenFalso")); // Verificamos el contenido
    }

    @Test
    void registro_DatosCorrectos_DeberiaDevolverUsuarioCreado() throws Exception {

        UserAddDTO nuevoUsuarioDTO = new UserAddDTO();
        nuevoUsuarioDTO.setNombreUsuario("Paco");
        nuevoUsuarioDTO.setApellidoUsuario("Gomez");
        nuevoUsuarioDTO.setEmailUsuario("paco@gmail.com");
        nuevoUsuarioDTO.setContrasenhaUsuario("12345");

        UsersAllDTO usuarioCreado = new UsersAllDTO();
        usuarioCreado.setIdUser(1L);
        usuarioCreado.setNombreUsuario("Paco");
        usuarioCreado.setEmailUsuario("paco@gmail.com");

        // simulamos el comportamiento del userservice
        given(userServiceImpl.addUsuario(any(UserAddDTO.class))).willReturn(usuarioCreado);

        //ejecución y verificación (when y then)
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevoUsuarioDTO)))
                .andExpect(status().isOk()) // esperamos un http 200
                .andExpect(jsonPath("$.id_usuario").value(1L))
                .andExpect(jsonPath("$.nombre_usuario").value("Paco"))
                .andExpect(jsonPath("$.correo_usuario").value("paco@gmail.com"));
    }
}
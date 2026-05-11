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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private UserServiceImpl userService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    // ─── Constantes reutilizables ────────────────────────────────────────────────
    private static final String EMAIL_VALIDO       = "paco@gmail.com";
    private static final String PASSWORD_VALIDO    = "12345";
    private static final String TOKEN_FALSO        = "eyJhbGciOiJIUzI1NiJ9.TokenFalso";
    private static final String COOKIE_JWT         = "jwt_token";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // LOGIN
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * El controlador extrae el token del Map, lo mete en una cookie HttpOnly
     * y lo ELIMINA del body. Por eso el body llega vacío ({}) y la cookie
     * Set-Cookie sí debe contener el token.
     */
    @Test
    void login_CredencialesCorrectas_DeberiaDevolverCookieConToken() throws Exception {

        LoginDTO loginDto = construirLoginDTO(EMAIL_VALIDO, PASSWORD_VALIDO);

        Map<String, Object> respuestaServicio = new HashMap<>();
        respuestaServicio.put("token", TOKEN_FALSO);

        given(authService.login(any())).willReturn(respuestaServicio);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                // HTTP 200
                .andExpect(status().isOk())
                // El token viaja en la cookie, NO en el body
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(COOKIE_JWT + "=" + TOKEN_FALSO)))
                // La cookie debe ser HttpOnly
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                // El body ya NO tiene el campo token (fue eliminado por el controlador)
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    /**
     * Campos en blanco → @Valid debe rechazar la petición con 400.
     */
    @Test
    void login_CamposVacios_DeberiaDevolver400() throws Exception {

        LoginDTO loginInvalido = construirLoginDTO("", "");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginInvalido)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Formato de email incorrecto → @Email rechaza con 400.
     */
    @Test
    void login_EmailMalFormado_DeberiaDevolver400() throws Exception {

        LoginDTO loginInvalido = construirLoginDTO("esto-no-es-un-email", PASSWORD_VALIDO);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginInvalido)))
                .andExpect(status().isBadRequest());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // REGISTRO
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Registro correcto: verifica HTTP 200 y los campos del body usando los
     * nombres JSON definidos con @JsonProperty en UsersAllDTO.
     */
    @Test
    void registro_DatosCorrectos_DeberiaDevolverUsuarioCreado() throws Exception {

        UserAddDTO nuevoUsuarioDTO = construirUserAddDTO(
                "Paco", "Gomez", EMAIL_VALIDO, PASSWORD_VALIDO);

        UsersAllDTO usuarioCreado = construirUsersAllDTO(1L, "Paco", "Gomez", EMAIL_VALIDO);

        given(userService.addUsuario(any())).willReturn(usuarioCreado);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevoUsuarioDTO)))
                .andExpect(status().isOk())
                // Nombres según @JsonProperty de UsersAllDTO
                .andExpect(jsonPath("$.id_usuario").value(1L))
                .andExpect(jsonPath("$.nombre_usuario").value("Paco"))
                .andExpect(jsonPath("$.apellido_usuario").value("Gomez"))
                .andExpect(jsonPath("$.correo_usuario").value(EMAIL_VALIDO));
    }

    /**
     * Email vacío en registro → @NotBlank rechaza con 400.
     */
    @Test
    void registro_EmailVacio_DeberiaDevolver400() throws Exception {

        UserAddDTO dtoInvalido = construirUserAddDTO("Paco", "Gomez", "", PASSWORD_VALIDO);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Email mal formado en registro → @Email rechaza con 400.
     */
    @Test
    void registro_EmailMalFormado_DeberiaDevolver400() throws Exception {

        UserAddDTO dtoInvalido = construirUserAddDTO("Paco", "Gomez", "no-es-email", PASSWORD_VALIDO);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoInvalido)))
                .andExpect(status().isBadRequest());
    }


    private LoginDTO construirLoginDTO(String email, String password) {
        LoginDTO dto = new LoginDTO();
        dto.setEmailUsuario(email);
        dto.setContrasenhaUsuario(password);
        return dto;
    }

    private UserAddDTO construirUserAddDTO(String nombre, String apellido,
                                           String email, String password) {
        UserAddDTO dto = new UserAddDTO();
        dto.setNombreUsuario(nombre);
        dto.setApellidoUsuario(apellido);
        dto.setEmailUsuario(email);
        dto.setContrasenhaUsuario(password);
        return dto;
    }

    private UsersAllDTO construirUsersAllDTO(Long id, String nombre,
                                             String apellido, String email) {
        UsersAllDTO dto = new UsersAllDTO();
        dto.setIdUser(id);
        dto.setNombreUsuario(nombre);
        dto.setApellidoUsuario(apellido);
        dto.setEmailUsuario(email);
        return dto;
    }
}
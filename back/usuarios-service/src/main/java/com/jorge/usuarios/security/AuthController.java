package com.jorge.usuarios.security;


import com.jorge.usuarios.dto.LoginDTO;
import com.jorge.usuarios.dto.UserAddDTO;
import com.jorge.usuarios.dto.UsersAllDTO;
import com.jorge.usuarios.exceptions.BadRequestException;
import com.jorge.usuarios.exceptions.ConflictException;
import com.jorge.usuarios.exceptions.DuplicateException;
import com.jorge.usuarios.services.AuthService;
import com.jorge.usuarios.services.impl.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;

import java.util.Map;

/**
 * Controlador REST público que gestiona la autenticación de la aplicación.
 * Proporciona los endpoints iniciales que no requieren de un Token JWT previo,
 * permitiendo a los usuarios registrarse y obtener credenciales de acceso.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Endpoints públicos para registro y login de usuarios.")
public class AuthController {
    private final AuthService authServiceImpl;
    private final UserServiceImpl userServiceImpl;

    public AuthController(AuthService authServiceImpl, UserServiceImpl userServiceImpl){
        this.userServiceImpl = userServiceImpl;
        this.authServiceImpl = authServiceImpl;
    }

    @Operation(summary = "Registrar nuevo usuario", description = "Crea un usuario en el sistema, encripta su contraseña y le asigna el rol ALUMNO por defecto.")
    @PostMapping("/register")
    public ResponseEntity<UsersAllDTO> registro(@Valid @RequestBody UserAddDTO dto) throws DuplicateException {
        return ResponseEntity.ok(userServiceImpl.addUsuario(dto));
    }

    @Operation(summary = "Iniciar sesión", description = "Valida las credenciales del usuario y devuelve un token JWT para acceder a los endpoints protegidos.")
    @PostMapping("/login")
    // FÍJATE AQUÍ: Cambiamos <String, String> por <String, Object>
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDto) throws ConflictException, BadRequestException {
        // 1. Delegamos toda la lógica de negocio al Service (¡Como debe ser!)
        Map<String, Object> loginData = authServiceImpl.login(loginDto);

        // 2. Extraemos el token del mapa para construir la Cookie
        String token = (String) loginData.get("token");

        // 3. El Controller fabrica la Cookie porque es un elemento HTTP
        ResponseCookie springCookie = ResponseCookie.from("jwt_token", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("None")
                .build();

        // 4. (Opcional/recomendado) Quitamos el token del body para que no llegue al JS
        loginData.remove("token");

        // 5. Devolvemos la respuesta HTTP montada
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, springCookie.toString())
                .body(loginData);
        }
    }

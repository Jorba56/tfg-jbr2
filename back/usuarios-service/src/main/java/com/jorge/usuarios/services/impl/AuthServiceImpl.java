package com.jorge.usuarios.services.impl;

import com.jorge.usuarios.entity.Item;
import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.repository.UserRepository;
import com.jorge.usuarios.dto.LoginDTO;
import com.jorge.usuarios.exceptions.BadRequestException;
import com.jorge.usuarios.exceptions.ConflictException;
import com.jorge.usuarios.security.JwtUtil;
import com.jorge.usuarios.services.AuthService;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio encargado de gestionar la lógica de negocio relacionada con la autenticación de usuarios.
 * Interactúa con la base de datos para validar credenciales y utiliza la utilidad JWT para la emisión de tokens.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRep;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRep, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRep = userRep;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Valida las credenciales proporcionadas por el usuario contra la base de datos.
     * Si las credenciales son correctas, genera y firma un nuevo Token JWT para su uso en futuras peticiones.
     *
     * @param loginDto Objeto de transferencia de datos que contiene el correo y la contraseña en texto plano.
     * @return Mapa clave-valor que contiene el Token JWT generado.
     */
    @Override
    @Transactional
    public Map<String, Object> login(LoginDTO loginDto) throws BadRequestException, ConflictException {
        User usuario = userRep.findUserByEmailUsuario(loginDto.getEmailUsuario());

        if (usuario == null || !passwordEncoder.matches(loginDto.getContrasenhaUsuario(), usuario.getContrasenhaUsuario())) {
            throw new BadRequestException("Credenciales de acceso incorrectas.");
        }
        if (!usuario.getActivo()) {
            throw new ConflictException("La cuenta de usuario se encuentra desactivada.");
        }

        List<String> rolesString = new ArrayList<>();
        for (Rol rol : usuario.getRoles()) {
            rolesString.add(rol.getName());
        }

        boolean isAdmin = rolesString.stream().anyMatch(r -> r.equalsIgnoreCase("ADMIN"));

        // Generamos el token
        String token = jwtUtil.generarToken(usuario.getEmailUsuario(), rolesString);

        // Empaquetamos lo que el Controller necesita
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("token", token); // Se lo pasamos para que haga la cookie
        respuesta.put("mensaje", "Login exitoso");
        respuesta.put("username", usuario.getNombreUsuario());
        respuesta.put("creditos", usuario.getCreditos());
        respuesta.put("isAdmin", isAdmin);

        return respuesta;
    }
}
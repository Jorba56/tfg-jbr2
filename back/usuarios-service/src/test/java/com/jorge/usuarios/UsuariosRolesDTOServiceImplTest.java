package com.jorge.usuarios;

import com.jorge.usuarios.dto.UsuarioRolesDTO;
import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.repository.UserRepository;
import com.jorge.usuarios.services.impl.UsuariosRolesDTOServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UsuariosRolesDTOServiceImplTest {

    @Mock
    private UserRepository userRep;

    @InjectMocks
    private UsuariosRolesDTOServiceImpl urService;

    @Test
    void listaUsuariosRoles_ConUsuarios() {
        // GIVEN: Preparamos un rol
        Rol rol = new Rol();
        rol.setIdRol(2L);
        List<Rol> roles = new ArrayList<>();
        roles.add(rol);

        // GIVEN: Preparamos un usuario y le metemos el rol dentro
        User usuario = new User();
        usuario.setIdUser(1L);
        usuario.setRoles(roles);

        List<User> listaUsuarios = new ArrayList<>();
        listaUsuarios.add(usuario);

        // Educamos a Mockito para que devuelva nuestra lista cuando busque usuarios activos
        given(userRep.findUsersByActivoIs(true)).willReturn(listaUsuarios);

        // WHEN: Ejecutamos el servicio
        List<UsuarioRolesDTO> resultado = urService.listaUsuariosRoles();

        // THEN: Comprobamos que el DTO se ha construido perfectamente
        assertNotNull(resultado);
        assertFalse(resultado.isEmpty());

        // Comprobamos el ID del usuario
        assertEquals(1L, resultado.getFirst().getIdUser());

        // Comprobamos la lista de IDs de roles dentro del DTO
        assertEquals(1, resultado.getFirst().getIdRoles().size());
        assertEquals(2L, resultado.getFirst().getIdRoles().getFirst());

        verify(userRep).findUsersByActivoIs(true);
    }

    @Test
    void listaUsuariosRoles_SinUsuarios() {
        // GIVEN: Simulamos que la base de datos no tiene usuarios
        given(userRep.findUsersByActivoIs(true)).willReturn(new ArrayList<>());

        // WHEN: Ejecutamos el servicio
        List<UsuarioRolesDTO> resultado = urService.listaUsuariosRoles();

        // THEN: Debería devolvernos una lista vacía sin dar errores (NullPointer)
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());

        verify(userRep).findUsersByActivoIs(true);
    }
}
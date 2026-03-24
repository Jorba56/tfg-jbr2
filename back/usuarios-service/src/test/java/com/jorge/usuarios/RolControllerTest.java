package com.jorge.usuarios;

import com.jorge.usuarios.controller.RolController;
import com.jorge.usuarios.dto.RolDTO;
import com.jorge.usuarios.dto.RolPutDTO;
import com.jorge.usuarios.dto.UserByRol;
import com.jorge.usuarios.entity.Rol;
import com.jorge.usuarios.entity.User;
import com.jorge.usuarios.exceptions.ConflictException;
import com.jorge.usuarios.services.impl.RolServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolControllerTest {

    @Mock
    private RolServiceImpl rolServiceImpl;

    @InjectMocks
    private RolController rolController;

    @Test
    void getAllRoles() {
        Rol rol1 = new Rol();
        rol1.setName("administrador");
        rol1.setActivo(true);
        rol1.setIdRol(4L);

        given(rolServiceImpl.listarRoles()).willReturn(List.of(rol1));

        List<Rol> rolList = rolController.getAllRoles();

        assertFalse(rolList.isEmpty());
        assertEquals(1, rolList.size());
        verify(rolServiceImpl).listarRoles();
        verifyNoMoreInteractions(rolServiceImpl);
    }

    @Test
    void getRolId() {
        Rol rol1 = new Rol();
        rol1.setName("administrador");
        rol1.setActivo(true);
        rol1.setIdRol(4L);

        given(rolServiceImpl.rolPorId(4L)).willReturn(Optional.of(rol1));

        //when
        Optional<Rol> rolSearch = rolController.getRolId(4L);

        assertTrue(rolSearch.isPresent());
        assertEquals(rol1.getIdRol(), rolSearch.get().getIdRol());
        verify(rolServiceImpl).rolPorId(4L);
        verifyNoMoreInteractions(rolServiceImpl);
    }

    @Test
    void addRol() {
        RolDTO rolDTO = new RolDTO();
        rolDTO.setName("nuevo_rol");

        given(rolServiceImpl.newRol(rolDTO)).willReturn("rol añadido con exito");

        String correcto = rolController.addRol(rolDTO);

        assertNotNull(correcto);
        assertEquals("rol añadido con exito", correcto);
        verify(rolServiceImpl).newRol(rolDTO);
        verifyNoMoreInteractions(rolServiceImpl);
    }

    @Test
    void updateRol() {
        RolPutDTO rolNuevo = new RolPutDTO();
        rolNuevo.setName("administrador2");
        rolNuevo.setActivo(true);

        given(rolServiceImpl.actualizarRol(4L, rolNuevo)).willReturn("Rol editado correctamente");

        String correcto = rolController.updateRol(4L, rolNuevo);

        assertNotNull(correcto);
        assertEquals("Rol editado correctamente", correcto);
        verify(rolServiceImpl).actualizarRol(4L, rolNuevo);
        verifyNoMoreInteractions(rolServiceImpl);
    }

    @Test
    void deleteRol() throws ConflictException {
        given(rolServiceImpl.desactivarRol(6L)).willReturn("rol borrado con éxito");

        //when
        String borrado = rolController.deleteRol(6L);

        //asserts
        assertNotNull(borrado);
        assertEquals("rol borrado con éxito", borrado);
        verify(rolServiceImpl).desactivarRol(6L);
        verifyNoMoreInteractions(rolServiceImpl);
    }

    @Test
    void getRolIdNull() {
        given(rolServiceImpl.rolPorId(99L)).willReturn(Optional.empty());

        Optional<Rol> rolSearch = rolController.getRolId(99L);

        assertTrue(rolSearch.isEmpty());
        verify(rolServiceImpl).rolPorId(99L);
    }

    @Test
    void userRol() {
        User usuario = new User();
        usuario.setIdUser(1L);
        usuario.setNombreUsuario("jorge");

        UserByRol usuario2 = new UserByRol();
        usuario2.setNombreUsuario("jorge");

        given(rolServiceImpl.userPorRol(2L)).willReturn(List.of(usuario2));

        List<UserByRol> usuarios = rolController.userRol(2L);
        UserByRol user1= usuarios.getFirst();

        assertFalse(usuarios.isEmpty());
        assertEquals("jorge", user1.getNombreUsuario());
        verify(rolServiceImpl).userPorRol(2L);
    }
}
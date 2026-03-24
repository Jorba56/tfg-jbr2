package com.jorge.usuarios;

import com.jorge.usuarios.controller.UsuariosRolesDTOController;
import com.jorge.usuarios.dto.UsuarioRolesDTO;
import com.jorge.usuarios.services.UsuariosRolesDTOService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UsuariosRolesDTOControllerTest {

    @Mock
    private UsuariosRolesDTOService urService;

    @InjectMocks
    private UsuariosRolesDTOController urController;

    @Test
    void getAllRoles() {
        //Preparamos un DTO simulado
        UsuarioRolesDTO dto = new UsuarioRolesDTO();
        dto.setIdUser(1L);
        List<Long> lista1= new ArrayList<>();
        lista1.add(2L);
        dto.setIdRoles(lista1);
        given(urService.listaUsuariosRoles()).willReturn(List.of(dto));

        //Llamamos al endpoint
        List<UsuarioRolesDTO> lista = urController.getAllRoles();

        //Comprobamos resultados
        assertFalse(lista.isEmpty());
        assertEquals(1L, lista.getFirst().getIdUser());
        assertEquals((2L), (lista.getFirst()).getIdRoles().getFirst());
        verify(urService).listaUsuariosRoles();
    }
}

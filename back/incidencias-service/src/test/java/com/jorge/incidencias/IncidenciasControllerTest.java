package com.jorge.incidencias;

import com.jorge.incidencias.controller.IncidenciasController;
import com.jorge.incidencias.entity.Incidencia;
import com.jorge.incidencias.services.IncidenciasService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IncidenciasControllerTest {

    @Mock
    private IncidenciasService incidenciasService;

    @InjectMocks
    private IncidenciasController incidenciasController;

    @Test
    void getAllIncidencias() {
        given(incidenciasService.obtenerTodas()).willReturn(List.of(new Incidencia()));
        List<Incidencia> res = incidenciasController.getAllIncidencias();
        assertFalse(res.isEmpty());
    }

    @Test
    void createIncidencia() {
        Incidencia i = new Incidencia();
        String res = incidenciasController.createIncidencia(i);
        assertEquals("Incidencia guardada correctamente.", res);
        verify(incidenciasService).guardar(i);
    }

    @Test
    void getIncidenciaById() {
        Incidencia i = new Incidencia();
        given(incidenciasService.obtenerPorId(1L)).willReturn(i);
        assertEquals(i, incidenciasController.getIncidenciaById(1L));
    }

    @Test
    void getIncidenciasByUsuario() {
        given(incidenciasService.obtenerPorUsuario(1L)).willReturn(List.of(new Incidencia()));
        assertFalse(incidenciasController.getIncidenciasByUsuario(1L).isEmpty());
    }

    @Test
    void getIncidenciasByClase() {
        given(incidenciasService.obtenerPorClase("UserService")).willReturn(List.of(new Incidencia()));
        assertFalse(incidenciasController.getIncidenciasByClase("UserService").isEmpty());
    }

    @Test
    void getIncidenciasByMetodo() {
        given(incidenciasService.obtenerPorMetodo("login")).willReturn(List.of(new Incidencia()));
        assertFalse(incidenciasController.getIncidenciasByMetodo("login").isEmpty());
    }
}
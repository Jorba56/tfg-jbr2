package com.jorge.incidencias;

import com.jorge.incidencias.controller.IncidenciasController;
import com.jorge.incidencias.entity.Incidencia;
import com.jorge.incidencias.exceptions.NotFoundException;
import com.jorge.incidencias.services.IncidenciasService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidenciasControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // configura el controlador aislado (asegúrate de que tu variable inyectada se llame userController)
        mockMvc = MockMvcBuilders.standaloneSetup(incidenciasController).build();
    }

    @Mock
    private IncidenciasService incidenciasService;

    @InjectMocks
    private IncidenciasController incidenciasController;

    @Test
    void getAllIncidencias() {
        Incidencia incidencia = new Incidencia();
        List<Incidencia> lista = List.of(incidencia);

        given(incidenciasService.obtenerTodas()).willReturn((lista));

        List<Incidencia> incidencias = incidenciasController.getAllIncidencias();

        assertFalse(incidencias.isEmpty());
        assertEquals(1, incidencias.size());
        verify(incidenciasService).obtenerTodas();
    }

    @Test
    void guardar() {
        Incidencia incidencia = new Incidencia();
        incidenciasController.createIncidencia(incidencia);
        verify(incidenciasService).guardar(incidencia);
    }

    @Test
    void obtenerPorId_Exito() {
        Incidencia incidencia = new Incidencia();
        given(incidenciasService.obtenerPorId(1L)).willReturn((incidencia));

        Incidencia resultado = incidenciasController.getIncidenciaById(1L);

        assertNotNull(resultado);
        verify(incidenciasService).obtenerPorId(1L);
    }

    @Test
    void obtenerPorClase_NoEncontrado() {
        // forzar lista vacia para entrar al if
        given(incidenciasService.obtenerPorClase("123")).willThrow(
                new NotFoundException("No se han encontrado incidencias originadas en la clase: 123")
        );

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> incidenciasController.getIncidenciasByClase("123") // Llamamos al controller, no al service
        );

        Assertions.assertEquals("No se han encontrado incidencias originadas en la clase: 123", ex.getMessage());
    }

    @Test
    void obtenerPorMetodo_Exito() {
        Incidencia incidencia = new Incidencia();
        given(incidenciasService.obtenerPorMetodo("login")).willReturn(List.of(incidencia));

        List<Incidencia> resultado = incidenciasController.getIncidenciasByMetodo("login");

        assertFalse(resultado.isEmpty());
        verify(incidenciasService).obtenerPorMetodo("login");
    }

    @Test
    void obtenerPorMetodo_NoEncontrado() {
        // forzar lista vacia para entrar al if
        given(incidenciasService.obtenerPorMetodo("metodoInventado")).willThrow(
                new NotFoundException("No se han encontrado incidencias originadas en el método: metodoInventado")
        );

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> incidenciasController.getIncidenciasByMetodo("metodoInventado")
        );

        Assertions.assertEquals("No se han encontrado incidencias originadas en el método: metodoInventado", ex.getMessage());
    }

    @Test
    void getIncidenciasByUsuario_Exito() {

        Incidencia incidencia = new Incidencia();
        List<Incidencia> listaEsperada = List.of(incidencia);

        given(incidenciasService.obtenerPorUsuario(1L)).willReturn(listaEsperada);

        List<Incidencia> resultado = incidenciasController.getIncidenciasByUsuario(1L);

        assertNotNull(resultado);
        assertFalse(resultado.isEmpty());
        verify(incidenciasService).obtenerPorUsuario(1L); // Verificamos que el controller llamó al service
    }

    @Test
    void getIncidenciasByUsuario_NoEncontrado() {

        given(incidenciasService.obtenerPorUsuario(99L)).willThrow(
                new NotFoundException("No se han encontrado incidencias para el usuario con ID: 99")
        );

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> incidenciasController.getIncidenciasByUsuario(99L)
        );

        Assertions.assertEquals("No se han encontrado incidencias para el usuario con ID: 99", ex.getMessage());
    }

    @Test
    void getIncidenciasByClaseTest() throws Exception {
        // 1. Preparamos los datos de mentira (Mock)
        String claseBuscada = "UserService";
        Incidencia incidenciaMock = new Incidencia();
        // incidenciaMock.setId(1L); // Opcional: setear algún dato si lo necesitas
        List<Incidencia> listaFalsa = List.of(incidenciaMock);

        // Le decimos al servicio falso lo que tiene que devolver
        when(incidenciasService.obtenerPorClase(claseBuscada)).thenReturn(listaFalsa);

        mockMvc.perform(get("/incidencias/clase/{clase}", claseBuscada)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.size()").value(1));

        // 4. Verificamos que el controlador llamó al servicio correctamente
        verify(incidenciasService, times(1)).obtenerPorClase(claseBuscada);
    }
}

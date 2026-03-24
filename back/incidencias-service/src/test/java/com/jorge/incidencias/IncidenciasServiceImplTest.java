package com.jorge.incidencias;

import com.jorge.incidencias.entity.Incidencia;
import com.jorge.incidencias.exceptions.NotFoundException;
import com.jorge.incidencias.repository.IncidenciasRepository;
import com.jorge.incidencias.services.impl.IncidenciasServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IncidenciasServiceImplTest {

    @Mock
    private IncidenciasRepository incidenciaRepository;

    @InjectMocks
    private IncidenciasServiceImpl  incidenciasService;

    // metodos generales

    @Test
    void obtenerTodas() {
        Incidencia incidencia = new Incidencia();
        List<Incidencia> lista = List.of(incidencia);

        given(incidenciaRepository.findAll()).willReturn(lista);

        List<Incidencia> resultado = incidenciasService.obtenerTodas();

        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
        verify(incidenciaRepository).findAll();
    }

    @Test
    void guardar() {
        Incidencia incidencia = new Incidencia();
        incidenciasService.guardar(incidencia);
        verify(incidenciaRepository).save(incidencia);
    }

    // filtros por ID

    @Test
    void obtenerPorId_Exito() {
        Incidencia incidencia = new Incidencia();
        given(incidenciaRepository.findById(1L)).willReturn(Optional.of(incidencia));

        Incidencia resultado = incidenciasService.obtenerPorId(1L);

        assertNotNull(resultado);
        verify(incidenciaRepository).findById(1L);
    }

    @Test
    void obtenerPorId_NoEncontrado() {
        given(incidenciaRepository.findById(99L)).willReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> incidenciasService.obtenerPorId(99L)
        );

        Assertions.assertEquals("No se ha encontrado ninguna incidencia con el ID: 99", ex.getMessage());
    }

    // filtros por usuario

    @Test
    void obtenerPorUsuario_Exito() {
        Incidencia incidencia = new Incidencia();
        given(incidenciaRepository.findByIdUsuario(1L)).willReturn(List.of(incidencia));

        List<Incidencia> resultado = incidenciasService.obtenerPorUsuario(1L);

        assertFalse(resultado.isEmpty());
        verify(incidenciaRepository).findByIdUsuario(1L);
    }

    @Test
    void obtenerPorUsuario_NoEncontrado() {
        // forzar lista vacia para entrar al if
        given(incidenciaRepository.findByIdUsuario(99L)).willReturn(new ArrayList<>());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> incidenciasService.obtenerPorUsuario(99L)
        );

        Assertions.assertEquals("No se han encontrado incidencias para el usuario con ID: 99", ex.getMessage());
    }

    // filtros por clase

    @Test
    void obtenerPorClase_Exito() {
        Incidencia incidencia = new Incidencia();
        given(incidenciaRepository.findByClase("UserService")).willReturn(List.of(incidencia));

        List<Incidencia> resultado = incidenciasService.obtenerPorClase("UserService");

        assertFalse(resultado.isEmpty());
        verify(incidenciaRepository).findByClase("UserService");
    }

    @Test
    void obtenerPorClase_NoEncontrado() {
        // forzar lista vacia para entrar al if
        given(incidenciaRepository.findByClase("InventadaService")).willReturn(new ArrayList<>());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> incidenciasService.obtenerPorClase("InventadaService")
        );

        Assertions.assertEquals("No se han encontrado incidencias originadas en la clase: InventadaService", ex.getMessage());
    }

    // filtros por metodo

    @Test
    void obtenerPorMetodo_Exito() {
        Incidencia incidencia = new Incidencia();
        given(incidenciaRepository.findByMetodo("login")).willReturn(List.of(incidencia));

        List<Incidencia> resultado = incidenciasService.obtenerPorMetodo("login");

        assertFalse(resultado.isEmpty());
        verify(incidenciaRepository).findByMetodo("login");
    }

    @Test
    void obtenerPorMetodo_NoEncontrado() {
        // forzar lista vacia para entrar al if
        given(incidenciaRepository.findByMetodo("metodoInventado")).willReturn(new ArrayList<>());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> incidenciasService.obtenerPorMetodo("metodoInventado")
        );

        Assertions.assertEquals("No se han encontrado incidencias originadas en el método: metodoInventado", ex.getMessage());
    }
}
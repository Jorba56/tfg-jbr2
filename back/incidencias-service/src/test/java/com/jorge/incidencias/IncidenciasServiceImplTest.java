package com.jorge.incidencias;

import com.jorge.incidencias.entity.Incidencia;
import com.jorge.incidencias.exceptions.NotFoundException;
import com.jorge.incidencias.repository.IncidenciasRepository;
import com.jorge.incidencias.services.impl.IncidenciasServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IncidenciasServiceImplTest {

    @Mock
    private IncidenciasRepository incidenciaRepository;

    @InjectMocks
    private IncidenciasServiceImpl incidenciasService;

    @Test
    void obtenerTodas() {
        given(incidenciaRepository.findAll()).willReturn(List.of(new Incidencia()));
        List<Incidencia> resultado = incidenciasService.obtenerTodas();
        assertFalse(resultado.isEmpty());
    }

    @Test
    void guardar() {
        Incidencia i = new Incidencia();
        incidenciasService.guardar(i);
        verify(incidenciaRepository).save(i);
    }

    @Test
    void obtenerPorId_Exito() {
        Incidencia i = new Incidencia();
        i.setIdIncidencia(1L);
        given(incidenciaRepository.findById(1L)).willReturn(Optional.of(i));

        Incidencia resultado = incidenciasService.obtenerPorId(1L);
        assertNotNull(resultado);
        assertEquals(1L, resultado.getIdIncidencia());
    }

    @Test
    void obtenerPorId_NotFound() {
        given(incidenciaRepository.findById(99L)).willReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> incidenciasService.obtenerPorId(99L));
        assertEquals("No se ha encontrado ninguna incidencia con el ID: 99", ex.getMessage());
    }

    @Test
    void obtenerPorUsuario_Exito() {
        given(incidenciaRepository.findByIdUsuario(1L)).willReturn(List.of(new Incidencia()));
        assertFalse(incidenciasService.obtenerPorUsuario(1L).isEmpty());
    }

    @Test
    void obtenerPorUsuario_NotFound() {
        given(incidenciaRepository.findByIdUsuario(99L)).willReturn(List.of());
        assertThrows(NotFoundException.class, () -> incidenciasService.obtenerPorUsuario(99L));
    }

    @Test
    void obtenerPorClase_Exito() {
        given(incidenciaRepository.findByClase("UserService")).willReturn(List.of(new Incidencia()));
        assertFalse(incidenciasService.obtenerPorClase("UserService").isEmpty());
    }

    @Test
    void obtenerPorClase_NotFound() {
        given(incidenciaRepository.findByClase("FakeClass")).willReturn(List.of());
        assertThrows(NotFoundException.class, () -> incidenciasService.obtenerPorClase("FakeClass"));
    }

    @Test
    void obtenerPorMetodo_Exito() {
        given(incidenciaRepository.findByMetodo("login")).willReturn(List.of(new Incidencia()));
        assertFalse(incidenciasService.obtenerPorMetodo("login").isEmpty());
    }

    @Test
    void obtenerPorMetodo_NotFound() {
        given(incidenciaRepository.findByMetodo("fakeMethod")).willReturn(List.of());
        assertThrows(NotFoundException.class, () -> incidenciasService.obtenerPorMetodo("fakeMethod"));
    }
}
package com.jorge.incidencias;

import com.jorge.incidencias.controller.GameController;
import com.jorge.incidencias.services.impl.GeminiServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private GeminiServiceImpl geminiService;

    @InjectMocks
    private GameController gameController;

    @Test
    void obtenerFrase() {
        given(geminiService.generarFrase("media", "F1")).willReturn("Frase generada|#");
        ResponseEntity<String> res = gameController.obtenerFrase("media", "F1");
        assertEquals(200, res.getStatusCode().value());
        assertEquals("Frase generada|#", res.getBody());
    }

    @Test
    void obtenerRoscoIA_Exito() {
        given(geminiService.generarRosco("Informática")).willReturn("[{\"letra\":\"A\",\"palabra\":\"API\"}]");
        ResponseEntity<String> res = gameController.obtenerRoscoIA("Informática");
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void obtenerRoscoIA_Vacio() {
        given(geminiService.generarRosco(anyString())).willReturn("[]");
        ResponseEntity<String> res = gameController.obtenerRoscoIA("Informática");
        assertEquals(400, res.getStatusCode().value());
        assertEquals("Error al generar el rosco en la IA", res.getBody());
    }

    @Test
    void obtenerRoscoIA_Null() {
        given(geminiService.generarRosco(anyString())).willReturn(null);
        ResponseEntity<String> res = gameController.obtenerRoscoIA("Informática");
        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void obtenerRoscoIA_Excepcion() {
        doThrow(new RuntimeException("Fallo catastrofico")).when(geminiService).generarRosco(anyString());
        ResponseEntity<String> res = gameController.obtenerRoscoIA("Informática");
        assertEquals(500, res.getStatusCode().value());
        assertEquals("Fallo interno en el servidor", res.getBody());
    }
}
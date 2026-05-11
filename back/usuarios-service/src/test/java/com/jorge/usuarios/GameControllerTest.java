package com.jorge.usuarios;

import com.jorge.usuarios.controller.GameController;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GameControllerTest {

    @Test
    void gestionarPartida() {
        GameController gameController = new GameController();
        Map<String, Object> payloadEntrada = new HashMap<>();
        payloadEntrada.put("accion", "START");
        payloadEntrada.put("jugador", "Paco");

        Map<String, Object> resultado = gameController.gestionarCarrera("SALA123", payloadEntrada);

        assertEquals(payloadEntrada, resultado);
        assertEquals("START", resultado.get("accion"));
    }
}
package com.jorge.usuarios.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class GameController {

    // 🚨 Atrapamos la variable {sala} que nos envíe el JavaScript
    @MessageMapping("/progreso/{sala}")
    @SendTo("/topic/partida/{sala}")
    public Map<String, Object> enviarProgreso(@DestinationVariable String sala, @Payload Map<String, Object> datosJugador) {

        System.out.println("🏁 Actualización en Sala Privada [" + sala + "]: " + datosJugador);

        return datosJugador;
    }

    @MessageMapping("/progreso/{sala}")
    @SendTo("/topic/partida/{sala}")
    public Map<String, Object> gestionarCarrera(@DestinationVariable String sala, @Payload Map<String, Object> payload) {
        // Si el mensaje es de tipo "ACCION" y la accion es "UNIRSE",
        // podemos enviar una señal de "START" a ambos.
        return payload;
    }
}
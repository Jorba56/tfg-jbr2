package com.jorge.incidencias.controller;

import com.jorge.incidencias.services.impl.GeminiServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
// Usamos /incidencias/game para que el API Gateway lo deje pasar automáticamente con tu configuración actual
@RequestMapping("/incidencias/game")
public class GameController {

    private final GeminiServiceImpl geminiService;

    public GameController(GeminiServiceImpl geminiService) {
        this.geminiService = geminiService;
    }

    @GetMapping("/frase")
    public ResponseEntity<String> obtenerFrase(@RequestParam(defaultValue = "media") String dificultad) {
        String frase = geminiService.generarFrase(dificultad);
        return ResponseEntity.ok(frase);
    }
}
package com.jorge.incidencias.controller;

import com.jorge.incidencias.services.impl.GeminiServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
// Usamos /incidencias/game para que el API Gateway lo deje pasar automáticamente con tu configuración actual
@RequestMapping("/incidencias/game")
public class GameController {

    private final GeminiServiceImpl geminiService;

    public GameController(GeminiServiceImpl geminiService) {
        this.geminiService = geminiService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/frase")
    public ResponseEntity<String> obtenerFrase(
        @RequestParam(defaultValue = "media") String dificultad,
        @RequestParam(required = false) String tema) {

        System.out.println("🏁 EL CONTROLADOR HA RECIBIDO EL TEMA: " + tema);
        String frase = geminiService.generarFrase(dificultad, tema);

        return ResponseEntity.ok(frase);
    }
}
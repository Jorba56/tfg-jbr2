package com.jorge.incidencias.controller;

import com.jorge.incidencias.services.impl.GeminiServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
// Usamos /incidencias/game para que el API Gateway lo deje pasar automáticamente con la configuración actual
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

    @GetMapping("/rosco-ia")
    public ResponseEntity<String> obtenerRoscoIA(@RequestParam(value = "tema", required = false) String tema) {
        try {
            System.out.println("\n--- 🧠 PIDIENDO ROSCO A GEMINI | TEMA: " + tema + " ---");

            String jsonRosco = geminiService.generarRosco(tema);

            // Si el servicio nos devuelve el array vacío, disparamos el 400
            if (jsonRosco == null || jsonRosco.equals("[]")) {
                System.out.println("❌ El servicio devolvió un rosco vacío. Abortando.");
                return ResponseEntity.badRequest().body("Error al generar el rosco en la IA");
            }

            System.out.println("✅ Rosco enviado al jugador con éxito.\n");
            return ResponseEntity.ok(jsonRosco);

        } catch (Exception e) {
            // SI HAY UN ERROR 500, ESTO LO CAPTURARÁ Y NOS DIRÁ POR QUÉ
            System.err.println("💥 ERROR CRÍTICO 500 EN EL CONTROLADOR: " + e.getMessage());
            e.printStackTrace(); // Esto imprime la línea exacta del fallo en Railway
            return ResponseEntity.internalServerError().body("Fallo interno en el servidor");
        }
    }
}
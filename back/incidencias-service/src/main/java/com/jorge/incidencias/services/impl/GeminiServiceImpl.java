package com.jorge.incidencias.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.incidencias.services.GeminiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiServiceImpl implements GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generarFrase(String dificultad) {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

            String[] temas = {
                    "la antigua Roma", "el espacio exterior", "los animales marinos",
                    "los inventos tecnológicos", "la naturaleza", "la gastronomía mundial",
                    "la inteligencia artificial", "el cuerpo humano", "la historia del arte",
                    "los dinosaurios", "la física cuántica", "mitología griega", "historia del fútbol",
                    "formula 1", "nba", "juegos olímpicos"
            };
            String temaAleatorio = temas[(int) (Math.random() * temas.length)];

            // 1. Prompt blindado: Directo, sin saltos de línea raros y exigiendo a la IA que no hable de más.
            String prompt = "Escribe 5 frases curiosas sobre " + temaAleatorio + ". " +
                    "Usa una gramática natural y fluida de 10 a 15 palabras por frase. " +
                    "REGLA ESTRICTA: Responde ÚNICAMENTE con las frases separadas por el carácter '|'. " +
                    "No incluyas saludos ni repitas instrucciones. " +
                    "Al final de todo el bloque, añade el símbolo '#'.";

            String requestBody = "{" +
                    "\"contents\": [{\"parts\": [{\"text\": \"" + prompt + "\"}] }]," +
                    "\"generationConfig\": {" +
                    "\"temperature\": 1.2," +
                    "\"maxOutputTokens\": 1000" +
                    "}" +
                    "}";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            try {
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                JsonNode root = objectMapper.readTree(response.getBody());
                String fraseGenerada = root.path("candidates").get(0)
                        .path("content")
                        .path("parts").get(0)
                        .path("text").asText().trim();

                System.out.println("TEMA ELEGIDO PARA ESTA TANDA: " + temaAleatorio);

                return fraseGenerada;

            } catch (Exception e) {
                // 2. Telemetría limpia en consola para ti (sin usar slf4j)
                System.err.println("Fallo en boxes al contactar con Gemini: " + e.getMessage());

                // 3. Fallback limpio para el jugador: Frases de emergencia con el formato correcto
                return "El servidor de IA está repostando combustible|Inténtalo de nuevo en unos segundos|El coche de seguridad está en pista|#";
            }
        }
}

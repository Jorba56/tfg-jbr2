package com.jorge.incidencias.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiServiceImpl {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generarFrase(String dificultad) {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

            // 1. Creamos una ruleta de temas para forzar a la IA a ser variada
            String[] temas = {
                    "la antigua Roma", "el espacio exterior", "los animales marinos",
                    "los inventos tecnológicos", "la naturaleza", "la gastronomía mundial",
                    "la inteligencia artificial", "el cuerpo humano", "la historia del arte",
                    "los dinosaurios", "la física cuántica", "mitología griega"
            };
            String temaAleatorio = temas[(int) (Math.random() * temas.length)];

            // 2. Inyectamos el tema en el prompt
            String prompt = "Genera 5 frases curiosas sobre un tema aleatorio. " +
                    "Separa cada frase con '|'. " +
                    "Al final de TODO el bloque de 5 frases, añade el símbolo '#'. " +
                    "Regla de oro: No dejes ninguna frase sin terminar.";

            String requestBody = "{" +
                    "\"contents\": [{\"parts\": [{\"text\": \"" + prompt + "\"}] }]," +
                    "\"generationConfig\": {" +
                    "\"temperature\": 0.4," +
                    "\"maxOutputTokens\": 800" + // ¡Doblamos el espacio!
                    "}" +
                    "}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 4. Hacer la petición POST a Gemini
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            // 5. Extraer solo el texto de la respuesta (Parsear el JSON)
            JsonNode root = objectMapper.readTree(response.getBody());
            String fraseGenerada = root.path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText().trim();

            return fraseGenerada;

        } catch (Exception e) {
            System.err.println("Error al contactar con Gemini: " + e.getMessage());
            // FALLBACK: Si falla el internet o la IA, devolvemos una frase de emergencia para que el juego no se rompa
            return  e.getMessage();
        }
    }
}

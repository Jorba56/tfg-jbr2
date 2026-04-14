package com.jorge.incidencias.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.incidencias.services.GeminiService;
// Importamos las herramientas de Logging y Random
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Random;

@Service
public class GeminiServiceImpl implements GeminiService {

    // 1. Creamos el Logger oficial de la clase (adiós System.out)
    private static final Logger logger = LoggerFactory.getLogger(GeminiServiceImpl.class);

    // 2. Instanciamos la clase Random (adiós Math.random)
    private final Random random = new Random();

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generarFrase(String dificultad, String temaPersonalizado) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview::generateContent?key=" + apiKey;

        String temaFinal = "";

        if (temaPersonalizado != null && !temaPersonalizado.trim().isEmpty()) {
            temaFinal = temaPersonalizado.trim();
        } else {
            String[] temas = {
                    "la antigua Roma", "el espacio exterior", "los animales marinos",
                    "los inventos tecnológicos", "la naturaleza", "la gastronomía mundial",
                    "la inteligencia artificial", "el cuerpo humano", "la historia del arte",
                    "los dinosaurios", "la física cuántica", "mitología griega", "historia del fútbol",
                    "formula 1", "nba", "juegos olímpicos"
            };
            // 3. Usamos nextInt() para coger un número entero limpio
            temaFinal = temas[random.nextInt(temas.length)];
        }

        String prompt = "Escribe 5 frases curiosas sobre " + temaFinal + ". " +
                "Usa una gramática natural y fluida de 10 a 15 palabras por frase. " +
                "REGLA ESTRICTA: Responde ÚNICAMENTE con las frases separadas por el carácter '|'. " +
                "No incluyas saludos ni repitas instrucciones. " +
                "Al final de todo el bloque, añade el símbolo '#'.";

        String requestBody = "{" +
                "\"contents\": [{\"parts\": [{\"text\": \"" + prompt + "\"}] }]," +
                "\"generationConfig\": {" +
                "\"temperature\": 0.7," +
                "\"maxOutputTokens\": 1500" +
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

            // 4. Usamos logger.info en lugar de System.out.println
            logger.info("TEMA ELEGIDO PARA ESTA TANDA: {}", temaFinal);

            return fraseGenerada;

        } catch (Exception e) {
            // 5. Usamos logger.error en lugar de System.err.println
            logger.error("Fallo en boxes al contactar con Gemini: {}", e.getMessage());
            return "El servidor de IA está repostando combustible|Inténtalo de nuevo en unos segundos|El coche de seguridad está en pista|#";
        }
    }

    public String generarRosco(String temaPersonalizado) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=" + apiKey;

        String prompt = "Actúa como un experto creador del juego Pasapalabra. " +
                "Genera 25 palabras exactas sobre el tema: " + temaPersonalizado + ". " +
                "Letras obligatorias: A, B, C, D, E, F, G, H, I, J, L, M, N, Ñ, O, P, Q, R, S, T, U, V, X, Y, Z. " +
                "IMPORTANTE: " +
                "- Cada 'palabra' debe ser UNA SOLA PALABRA sin espacios. " +
                "- La 'definicion' debe ser UN SOLO PÁRRAFO sin saltos de línea. " +
                "- El idioma es estrictamente ESPAÑOL. " +
                "- Formato: {\"letra\": \"A\", \"palabra\": \"ejemplo\", \"definicion\": \"Empieza por A: texto aquí sin saltos de línea\"}. " +
                "Responde SOLO con el array JSON, nada más.";

        String escapedPrompt = escapeJsonString(prompt);

        String requestBody = "{" +
                "\"contents\": [{\"parts\": [{\"text\": \"" + escapedPrompt + "\"}]}]," +
                "\"generationConfig\": {" +
                "\"temperature\": 0.7," +
                "\"maxOutputTokens\": 3000," +
                "\"responseMimeType\": \"application/json\"" +
                "}" +
                "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            logger.info("📤 Enviando request a Gemini para tema: {}", temaPersonalizado);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("❌ Error HTTP: {} - {}", response.getStatusCode(), response.getBody());
                return "[]";
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.has("error")) {
                logger.error("🛑 Error de Gemini API: {}", root.path("error").asText());
                return "[]";
            }

            if (root.path("candidates").isEmpty()) {
                logger.error("🛑 Respuesta vacía de Gemini");
                return "[]";
            }

            // 1️⃣ EXTRAER TEXTO RAW
            String jsonCrudo = root.path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText().trim();

            logger.info("📦 Respuesta raw de Gemini (primeros 500 chars): {}",
                    jsonCrudo.length() > 500 ? jsonCrudo.substring(0, 500) : jsonCrudo);

            // 2️⃣ LIMPIAR MARKDOWN SI EXISTE
            if (jsonCrudo.contains("```")) {
                int inicioArray = jsonCrudo.indexOf("[");
                int finArray = jsonCrudo.lastIndexOf("]");
                if (inicioArray != -1 && finArray != -1) {
                    jsonCrudo = jsonCrudo.substring(inicioArray, finArray + 1);
                }
            }

            // 3️⃣ ESCAPAR SALTOS DE LÍNEA DENTRO DE STRINGS
            // Reemplazar \n y \r dentro de las definiciones
            jsonCrudo = jsonCrudo.replace("\n", " ")
                    .replace("\r", " ")
                    .replace("  ", " ");  // Eliminar espacios dobles

            logger.info("🧹 JSON limpiado (primeros 500 chars): {}",
                    jsonCrudo.length() > 500 ? jsonCrudo.substring(0, 500) : jsonCrudo);

            // 4️⃣ VALIDAR Y PARSEAR
            try {
                JsonNode arrayNode = objectMapper.readTree(jsonCrudo);

                if (!arrayNode.isArray()) {
                    logger.error("❌ La respuesta no es un array JSON válido");
                    return "[]";
                }

                if (arrayNode.size() != 25) {
                    logger.warn("⚠️  Se esperaban 25 palabras, se obtuvieron: {}", arrayNode.size());
                }

                logger.info("✅ ROSCO GENERADO CON ÉXITO - {} palabras para tema: {}",
                        arrayNode.size(), temaPersonalizado);
                return jsonCrudo;

            } catch (JsonProcessingException e) {
                logger.error("📄 Error al parsear JSON después de limpiar: {}", e.getMessage());
                logger.error("JSON problemático: {}", jsonCrudo.substring(0, Math.min(1000, jsonCrudo.length())));
                return "[]";
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("🌐 Error HTTP en Gemini API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "[]";
        } catch (Exception e) {
            logger.error("💥 Error inesperado: {}", e.getMessage(), e);
            return "[]";
        }
    }

    private String escapeJsonString(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
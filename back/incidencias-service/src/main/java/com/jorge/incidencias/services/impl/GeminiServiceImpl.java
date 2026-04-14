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
                "REQUISITOS CRÍTICOS: " +
                "1. Cada 'definicion' DEBE ser una sola línea, sin saltos de línea. Si es larga, usa puntos suspensivos. " +
                "2. Máximo 200 caracteres por definición. " +
                "3. Devuelve SOLO el JSON sin formato, en una sola línea si es posible. " +
                "4. Ejemplo exacto: [{\"letra\":\"A\",\"palabra\":\"Árbitro\",\"definicion\":\"Empieza por A: Juez...\"}, ...] ";

        String escapedPrompt = escapeJsonString(prompt);

        String requestBody = "{" +
                "\"contents\": [{\"parts\": [{\"text\": \"" + escapedPrompt + "\"}]}]," +
                "\"generationConfig\": {" +
                "\"temperature\": 0.7," +
                "\"maxOutputTokens\": 2500," +
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

            // EXTRAER TEXTO RAW
            String jsonCrudo = root.path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            logger.info("📦 Respuesta raw recibida, limpiando...");

            // 🔧 LIMPIEZA AGRESIVA
            String jsonLimpio = limpiarJsonDesdeBrokenNewlines(jsonCrudo);

            logger.info("✅ JSON limpiado y validado. Primeros 300 chars: {}",
                    jsonLimpio.length() > 300 ? jsonLimpio.substring(0, 300) : jsonLimpio);

            // VALIDAR Y PARSEAR
            JsonNode arrayNode = objectMapper.readTree(jsonLimpio);

            if (!arrayNode.isArray()) {
                logger.error("❌ La respuesta no es un array JSON válido");
                return "[]";
            }

            logger.info("✅ ROSCO GENERADO CON ÉXITO - {} palabras para tema: {}",
                    arrayNode.size(), temaPersonalizado);
            return jsonLimpio;

        } catch (JsonProcessingException e) {
            logger.error("📄 Error al parsear JSON: {}", e.getMessage());
            return "[]";
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("🌐 Error HTTP en Gemini API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "[]";
        } catch (Exception e) {
            logger.error("💥 Error inesperado: {}", e.getMessage(), e);
            return "[]";
        }
    }

    /**
     * Limpia un JSON roto con saltos de línea dentro de strings
     * Reconstruye el JSON de forma segura eliminando newlines problemáticos
     */
    private String limpiarJsonDesdeBrokenNewlines(String jsonRoto) {
        // 1. Remover espacios en blanco al inicio/final
        String cleaned = jsonRoto.trim();

        // 2. Remover markdown si existe
        if (cleaned.contains("```")) {
            int start = cleaned.indexOf("[");
            int end = cleaned.lastIndexOf("]");
            if (start != -1 && end != -1 && start < end) {
                cleaned = cleaned.substring(start, end + 1);
            }
        }

        // 3. 🔨 RECONSTRUIR EL JSON LÍNEA POR LÍNEA
        // Esto es lo clave: procesa cada línea y reconstruye sin saltos
        StringBuilder resultado = new StringBuilder();
        String[] lineas = cleaned.split("\n");

        for (String linea : lineas) {
            // Trim de cada línea y concatenar sin saltos
            String lineaTrimmed = linea.trim();
            if (!lineaTrimmed.isEmpty()) {
                resultado.append(lineaTrimmed);
            }
        }

        String jsonUnaLinea = resultado.toString();

        // 4. 🧹 Limpiar espacios múltiples
        jsonUnaLinea = jsonUnaLinea.replaceAll("\\s+", " ");

        // 5. ✅ Asegurar que empiece con [ y termine con ]
        if (!jsonUnaLinea.startsWith("[")) {
            int idx = jsonUnaLinea.indexOf("[");
            if (idx != -1) jsonUnaLinea = jsonUnaLinea.substring(idx);
        }
        if (!jsonUnaLinea.endsWith("]")) {
            int idx = jsonUnaLinea.lastIndexOf("]");
            if (idx != -1) jsonUnaLinea = jsonUnaLinea.substring(0, idx + 1);
        }

        logger.debug("🔧 JSON después de limpieza: {}", jsonUnaLinea);
        return jsonUnaLinea;
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
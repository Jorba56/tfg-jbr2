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

import java.util.*;

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
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=" + apiKey;

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

        // Prompt estructurado para que devuelva líneas limpias
        String prompt = "Actúa como un experto creador del juego Pasapalabra sobre: " + temaPersonalizado + ". " +
                "Devuelve EXACTAMENTE 25 palabras, UNA POR LÍNEA, con este formato: " +
                "LETRA|PALABRA|DEFINICION " +
                "(sin comillas, separado por barras verticales). " +
                "Letras obligatorias: A, B, C, D, E, F, G, H, I, J, L, M, N, Ñ, O, P, Q, R, S, T, U, V, X, Y, Z. " +
                "Ejemplo: A|Árbitro|Empieza por A: Persona que arbitra un partido. " +
                "IMPORTANTE: Cada definición en UNA sola línea, máximo 150 caracteres, SIN saltos de línea.";

        String escapedPrompt = escapeJsonString(prompt);

        // SIN responseMimeType, solo texto plano
        String requestBody = "{" +
                "\"contents\": [{\"parts\": [{\"text\": \"" + escapedPrompt + "\"}]}]," +
                "\"generationConfig\": {" +
                "\"temperature\": 2.0," +
                "\"maxOutputTokens\": 21000" +
                "}" +
                "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            logger.info("📤 Enviando request a Gemini para tema: {}", temaPersonalizado);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("❌ Error HTTP: {}", response.getStatusCode());
                return "[]";
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.has("error")) {
                logger.error("🛑 Error de Gemini API: {}", root.path("error").asText());
                return "[]";
            }

            if (root.path("candidates").isEmpty()) {
                logger.error("🛑 Respuesta vacía");
                return "[]";
            }

            String respuestaTexto = root.path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            logger.info("📦 Respuesta recibida ({} caracteres)", respuestaTexto.length());
            logger.debug("🔍 Contenido:\n{}", respuestaTexto);

            // Convertir formato texto a JSON
            String jsonResultado = convertirTextoAJson(respuestaTexto, temaPersonalizado);

            if (jsonResultado.equals("[]")) {
                logger.error("❌ No se pudieron procesar las palabras");
                return "[]";
            }

            logger.info("✅ ROSCO GENERADO CON ÉXITO - {} palabras", contarLineas(jsonResultado));
            return jsonResultado;

        } catch (Exception e) {
            logger.error("💥 Error: {}", e.getMessage(), e);
            return "[]";
        }
    }

    /**
     * Convierte el formato LETRA|PALABRA|DEFINICION a JSON array
     */
    private String convertirTextoAJson(String texto, String tema) {
        List<Map<String, String>> palabras = new ArrayList<>();

        // Dividir por líneas
        String[] lineas = texto.split("\n");

        logger.info("🔍 Procesando {} líneas", lineas.length);

        for (String linea : lineas) {
            linea = linea.trim();

            // Ignorar líneas vacías o que no tengan el formato correcto
            if (linea.isEmpty() || !linea.contains("|")) {
                continue;
            }

            // Dividir por |
            String[] partes = linea.split("\\|");

            if (partes.length >= 3) {
                String letra = partes[0].trim();
                String palabra = partes[1].trim();
                String definicion = partes[2].trim();

                // Validar que letra sea un carácter único
                if (letra.length() == 1 && !palabra.isEmpty() && !definicion.isEmpty()) {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("letra", letra.toUpperCase());
                    item.put("palabra", palabra);
                    item.put("definicion", definicion);
                    palabras.add(item);

                    logger.debug("✓ Añadida palabra: {} - {}", letra, palabra);
                }
            }
        }

        logger.info("📊 Se extrajeron {} palabras válidas", palabras.size());

        if (palabras.isEmpty()) {
            return "[]";
        }

        // Convertir a JSON
        try {
            return objectMapper.writeValueAsString(palabras);
        } catch (Exception e) {
            logger.error("❌ Error al serializar a JSON: {}", e.getMessage());
            return "[]";
        }
    }

    /**
     * Cuenta líneas en el JSON resultado
     */
    private int contarLineas(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return root.isArray() ? root.size() : 0;
        } catch (Exception e) {
            return 0;
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
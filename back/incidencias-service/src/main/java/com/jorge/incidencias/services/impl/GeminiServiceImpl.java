package com.jorge.incidencias.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.incidencias.services.GeminiService;
// Importamos las herramientas de Logging y Random
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
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
        // Te recomiendo usar gemini-1.5-flash (es el modelo estándar más rápido y listo actualmente)
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=" + apiKey;

        // Un prompt estructurado con viñetas es mucho más fácil de procesar para la IA
        String prompt = "Actúa como un experto creador del juego Pasapalabra. " +
                "Genera 25 palabras exactas sobre el tema: '" + temaPersonalizado + "'. " +
                "Letras obligatorias: A, B, C, D, E, F, G, H, I, J, L, M, N, Ñ, O, P, Q, R, S, T, U, V, X, Y, Z. " +
                "REGLAS: " +
                "1. La 'palabra' DEBE ser una única palabra (sin espacios). " +
                "2. El idioma es estrictamente ESPAÑOL. " +
                "3. La 'definicion' debe empezar siempre indicando la letra, por ejemplo: 'Empieza por A: ...' o 'Contiene la X: ...'. " +
                "4. Debes devolver un array de JSON donde cada objeto tenga exactamente estas claves: 'letra', 'palabra', 'definicion'.";

        // 🚨 LA MAGIA: Añadimos responseMimeType: application/json
        String requestBody = "{" +
                "\"contents\": [{\"parts\": [{\"text\": \"" + prompt + "\"}] }]," +
                "\"generationConfig\": {" +
                "\"temperature\": 0.3, " +
                "\"maxOutputTokens\": 2500, " +
                "\"responseMimeType\": \"application/json\"" +
                "}" +
                "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            // 1. Chivato antiescudos de seguridad
            if (root.path("candidates").isEmpty()) {
                logger.error("🛑 Gemini bloqueó la respuesta o devolvió vacío: {}", response.getBody());
                return "[]";
            }

            // Como forzamos application/json, el texto que devuelve ya es un JSON puro sin backticks (```json)
            String jsonCrudo = root.path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText().trim();

            // 2. Filtro extremo antibasura (Mantenido por extrema seguridad, aunque con JSON mode rara vez hace falta)
            int inicioArray = jsonCrudo.indexOf("[");
            int finArray = jsonCrudo.lastIndexOf("]");

            if (inicioArray != -1 && finArray != -1) {
                jsonCrudo = jsonCrudo.substring(inicioArray, finArray + 1);
            }

            logger.info("🏁 ROSCO GENERADO CON ÉXITO PARA EL TEMA: {}", temaPersonalizado);
            return jsonCrudo;

        } catch (Exception e) {
            logger.error("💥 FALLO CRÍTICO AL GENERAR ROSCO. Motivo exacto: {}", e.getMessage());
            return "[]";
        }
    }
}
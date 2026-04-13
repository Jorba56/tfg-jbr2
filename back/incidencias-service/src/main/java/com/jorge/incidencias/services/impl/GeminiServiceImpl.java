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
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=" + apiKey;

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
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro:generateContent?key=" + apiKey;

        String temaFinal = (temaPersonalizado != null && !temaPersonalizado.trim().isEmpty())
                ? temaPersonalizado : "cultura general y curiosidades";

        // El prompt es una obra de ingeniería estricta para que el JSON no falle
        String prompt = "Genera un juego de Pasapalabra sobre el tema: '" + temaFinal + "'. " +
                "Crea 25 palabras (UNICA Y EXCLUSIVAMENTE UNA PALABRA), una para cada letra: A, B, C, D, E, F, G, H, I, J, L, M, N, Ñ, O, P, Q, R, S, T, U, V, X, Y, Z. " +
                "Evita palabras en inglés. centrate en el español. " +
                "Di EXPLÍCITAMENTE si la palabra contiene o empieza con la letra que toca. " +
                "REGLA ESTRICTA: Responde ÚNICAMENTE con un array JSON crudo. No añadas saludos. " +
                "El formato de cada objeto debe ser: {'letra': 'A', 'palabra': '...', 'definicion': '...'}";

        // Cuerpo de la petición perfectamente encapsulado
        String requestBody = "{" +
                "\"contents\": [{\"parts\": [{\"text\": \"" + prompt + "\"}] }]," +
                "\"generationConfig\": {\"temperature\": 0.7, \"maxOutputTokens\": 2500}" +
                "}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            // 1. Chivato antiescudos de seguridad (A veces Gemini bloquea temas sin avisar)
            if (root.path("candidates").isEmpty()) {
                logger.error("🛑 Gemini bloqueó la respuesta o devolvió vacío: {}", response.getBody());
                return "[]";
            }

            String jsonCrudo = root.path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText().trim();

            // 2. FILTRO EXTREMO ANTIBASURA:
            // Si Gemini mete texto antes o después del JSON, lo recortamos de cuajo.
            int inicioArray = jsonCrudo.indexOf("[");
            int finArray = jsonCrudo.lastIndexOf("]");

            if (inicioArray != -1 && finArray != -1) {
                jsonCrudo = jsonCrudo.substring(inicioArray, finArray + 1);
            }

            logger.info("🏁 ROSCO GENERADO CON ÉXITO PARA EL TEMA: {}", temaFinal);
            return jsonCrudo;

        } catch (Exception e) {
            // 3. Telemetría de alta precisión
            logger.error("💥 FALLO CRÍTICO AL GENERAR ROSCO. Motivo exacto: {}", e.getMessage());
            return "[]"; // Activará el error 400 en el controlador
        }
    }
}
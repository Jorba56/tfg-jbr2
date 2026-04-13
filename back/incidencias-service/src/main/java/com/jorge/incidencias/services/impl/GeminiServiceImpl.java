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
import java.util.logging.Level;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

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
        // 🏎️ Usamos el motor ultrarresistente y oficial (1.5-flash)
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview:generateContent?key=" + apiKey;

        String temaFinal = (temaPersonalizado != null && !temaPersonalizado.trim().isEmpty())
                ? temaPersonalizado : "cultura general y curiosidades";

        // El prompt ahora es mucho más simple porque el esquema JSON hace el trabajo duro
        String prompt = "Genera un juego de Pasapalabra sobre el tema: '" + temaFinal + "'. " +
                "Crea exactamente 25 palabras en español, una para cada letra del abecedario (incluyendo la Ñ). " +
                "REGLA DE ORO: No uses NUNCA comillas dobles (\") dentro de las definiciones ni de las palabras, usa comillas simples (') si es necesario. " +
                "La definición debe empezar diciendo si 'Empieza por' o 'Contiene' la letra.";

        try {
            // 🛡️ EL ESCUDO DEFINITIVO: DEFINIMOS LA ESTRUCTURA EXACTA DEL JSON
            // Obligamos a la IA a rellenar este molde, es imposible que falle la sintaxis.
            Map<String, Object> schemaProperties = Map.of(
                    "letra", Map.of("type", "STRING"),
                    "palabra", Map.of("type", "STRING"),
                    "definicion", Map.of("type", "STRING")
            );

            Map<String, Object> schemaItem = Map.of(
                    "type", "OBJECT",
                    "properties", schemaProperties,
                    "required", List.of("letra", "palabra", "definicion")
            );

            Map<String, Object> responseSchema = Map.of(
                    "type", "ARRAY",
                    "items", schemaItem
            );

            Map<String, Object> requestMap = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of(
                            "temperature", 0.3,
                            "maxOutputTokens", 10000, // ⛽ Ampliamos el depósito para que nunca se corte a medias
                            "responseMimeType", "application/json",
                            "responseSchema", responseSchema // 🔒 APLICAMOS EL MOLDE ESTRICTO
                    )
            );

            String requestBody = objectMapper.writeValueAsString(requestMap);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.path("candidates").isEmpty()) {
                logger.error("🛑 Gemini devolvió vacío.");
                return "[]";
            }

            String jsonCrudo = root.path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText().trim();

            // 🧹 ESCOBA NIVEL PRO:
            // 1. Quitamos saltos de línea físicos que rompen el JSON
            jsonCrudo = jsonCrudo.replace("\n", " ").replace("\r", " ");

            // 2. 🛡️ PARCHE DE COMILLAS INTERNAS:
            // Esto es magia negra: busca comillas dobles que NO vayan seguidas de coma, llave o corchete
            // y las cambia por comillas simples para que no rompan el string del JSON.
            jsonCrudo = jsonCrudo.replaceAll("(?<![:\\[\\{,])\"(?![:,\\]\\}])", "'");

            logger.info("🏁 ROSCO GENERADO CON ÉXITO PARA EL TEMA: " + temaFinal);
            return jsonCrudo.trim();

        } catch (Exception e) {
            logger.error("💥 FALLO CRÍTICO AL GENERAR ROSCO. Motivo exacto: {}", e.getMessage());
            return "[]";
        }
    }
}
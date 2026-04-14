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

import java.util.ArrayList;
import java.util.List;
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
                "REQUISITOS: " +
                "1. Devuelve ÚNICAMENTE un JSON array, nada de markdown. " +
                "2. Cada definición máximo 180 caracteres, SIN saltos de línea dentro de los strings. " +
                "3. Formato exacto: [{\"letra\":\"A\",\"palabra\":\"ejemplo\",\"definicion\":\"Empieza por A: descripción...\"}, ...]";

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
                logger.error("🛑 Respuesta vacía de Gemini (sin candidates)");
                logger.error("🔍 Respuesta completa: {}", response.getBody());
                return "[]";
            }

            // EXTRAER TEXTO RAW
            String jsonCrudo = root.path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            logger.info("📦 Respuesta raw recibida ({} caracteres)", jsonCrudo.length());

            // 🚨 MOSTRAR LA RESPUESTA COMPLETA PARA DEBUGGEAR
            logger.error("🔴 CONTENIDO COMPLETO DE LA RESPUESTA:");
            logger.error("---START---");
            logger.error(jsonCrudo);
            logger.error("---END---");

            // 🔧 LIMPIEZA
            String jsonLimpio = procesarJsonDeLaIA(jsonCrudo);

            if (jsonLimpio.equals("[]")) {
                logger.error("❌ JSON limpio está vacío");
                return "[]";
            }

            // VALIDAR Y PARSEAR
            JsonNode arrayNode = objectMapper.readTree(jsonLimpio);

            if (!arrayNode.isArray()) {
                logger.error("❌ La respuesta no es un array JSON válido");
                return "[]";
            }

            if (arrayNode.size() == 0) {
                logger.error("❌ Array vacío");
                return "[]";
            }

            logger.info("✅ ROSCO GENERADO CON ÉXITO - {} palabras para tema: {}",
                    arrayNode.size(), temaPersonalizado);
            return jsonLimpio;

        } catch (JsonProcessingException e) {
            logger.error("📄 Error al parsear JSON: {}", e.getMessage());
            return "[]";
        } catch (Exception e) {
            logger.error("💥 Error: {}", e.getMessage(), e);
            return "[]";
        }
    }

    /**
     * Procesa JSON de Gemini: extrae objetos válidos y reconstruye el array
     */
    private String procesarJsonDeLaIA(String jsonRoto) {
        // 1. Remover markdown si existe
        String cleaned = jsonRoto.trim();
        if (cleaned.contains("```")) {
            int start = cleaned.indexOf("[");
            int end = cleaned.lastIndexOf("]");
            if (start != -1 && end != -1 && start < end) {
                cleaned = cleaned.substring(start, end + 1);
            }
        }

        // 2. Normalizar espacios y saltos de línea FUERA de strings
        // Estrategia: reconocer objetos completos { ... }
        List<String> objetos = extraerObjetos(cleaned);

        if (objetos.isEmpty()) {
            logger.error("❌ No se encontraron objetos JSON válidos");
            return "[]";
        }

        // 3. Reconstruir array limpio
        StringBuilder arrayReconstruido = new StringBuilder("[");
        for (int i = 0; i < objetos.size(); i++) {
            arrayReconstruido.append(objetos.get(i));
            if (i < objetos.size() - 1) {
                arrayReconstruido.append(",");
            }
        }
        arrayReconstruido.append("]");

        return arrayReconstruido.toString();
    }

    /**
     * Extrae objetos JSON individuales del texto, ignorando saltos de línea rotos
     */
    private List<String> extraerObjetos(String json) {
        List<String> objetos = new ArrayList<>();
        int nivel = 0;
        StringBuilder objetoActual = new StringBuilder();
        boolean enString = false;
        char charAnterior = ' ';

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            // Detectar si estamos dentro de un string (ignorar {}, [], etc. dentro de strings)
            if (c == '"' && charAnterior != '\\') {
                enString = !enString;
            }

            if (!enString) {
                if (c == '{') {
                    nivel++;
                } else if (c == '}') {
                    nivel--;
                }
            }

            // Agregar carácter si no es salto de línea innecesario
            if (c == '\n' || c == '\r') {
                // Solo añadir espacio si estamos dentro de un objeto y es necesario
                if (nivel > 0 && !enString && objetoActual.length() > 0) {
                    char ultimoChar = objetoActual.charAt(objetoActual.length() - 1);
                    if (ultimoChar != ' ' && ultimoChar != '{' && ultimoChar != '[' && ultimoChar != ':' && ultimoChar != ',') {
                        objetoActual.append(" ");
                    }
                }
            } else {
                objetoActual.append(c);
            }

            // Cuando cerramos un objeto, guardarlo
            if (nivel == 0 && objetoActual.length() > 0 && c == '}') {
                String objeto = objetoActual.toString().trim();
                if (objeto.startsWith("{") && objeto.endsWith("}")) {
                    objetos.add(objeto);
                    objetoActual = new StringBuilder();
                }
            }

            charAnterior = c;
        }

        logger.debug("🔍 Se extrajeron {} objetos válidos", objetos.size());
        return objetos;
    }

    /**
     * Cuenta objetos { } en el JSON (para logging)
     */
    private int contarObjetos(String json) {
        int count = 0;
        for (char c : json.toCharArray()) {
            if (c == '{') count++;
        }
        return count;
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
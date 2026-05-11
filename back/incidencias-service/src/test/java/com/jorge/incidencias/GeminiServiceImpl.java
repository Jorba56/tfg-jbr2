package com.jorge.incidencias;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.incidencias.services.impl.GeminiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class GeminiServiceImplTest {

    @InjectMocks
    private GeminiServiceImpl geminiService;

    @Mock
    private RestTemplate restTemplateMock;

    @Mock
    private ObjectMapper objectMapperMock;

    @BeforeEach
    void setUp() {
        // Forzamos la inyección de los Mocks en los campos privados y finales del servicio
        ReflectionTestUtils.setField(geminiService, "apiKey", "AIzaFakeKey_123");
        ReflectionTestUtils.setField(geminiService, "restTemplate", restTemplateMock);
        ReflectionTestUtils.setField(geminiService, "objectMapper", objectMapperMock);
    }

    @Test
    void generarFrase_Exito_TemaPersonalizado() throws Exception {
        String body = "{\"dummy\":\"json\"}";
        JsonNode rootNode = new ObjectMapper().readTree("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Frase 1|Frase 2|#\"}]}}]}");

        given(restTemplateMock.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>(body, HttpStatus.OK));
        given(objectMapperMock.readTree(anyString())).willReturn(rootNode);

        String resultado = geminiService.generarFrase("media", "Fórmula 1");

        assertEquals("Frase 1|Frase 2|#", resultado);
    }

    @Test
    void generarFrase_Exito_TemaAleatorio() throws Exception {
        // Pasamos null y vacío para cubrir las ramas del else del tema
        JsonNode rootNode = new ObjectMapper().readTree("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Frase random|#\"}]}}]}");

        given(restTemplateMock.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>("{}", HttpStatus.OK));
        given(objectMapperMock.readTree(anyString())).willReturn(rootNode);

        String resultado = geminiService.generarFrase("facil", "");
        assertNotNull(resultado);
        assertTrue(resultado.contains("#"));
    }

    @Test
    void generarFrase_CatchException() {
        // Forzamos el catch para cubrir la rama del error de red
        given(restTemplateMock.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willThrow(new RuntimeException("Fallo de red"));

        String resultado = geminiService.generarFrase("dificil", "Espacio");
        assertTrue(resultado.contains("repostando combustible"));
    }

    @Test
    void generarRosco_ExitoCompleto() throws Exception {
        // ARREGLO: Escapamos los saltos de línea (\\n) para que el JSON sea válido
        String textoIA = "A|Abeja|Definición A\\nB|Barco|Definición B\\nLineaErronea\\nC|Coche|Definicion C";

        // Construimos el JSON simulado que devolvería la API de Google
        String jsonSimulado = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + textoIA + "\"}]}}]}";

        // Preparamos un nodo real para que el mock del ObjectMapper lo devuelva
        // Usamos una instancia real de ObjectMapper solo para crear el nodo del test
        ObjectMapper mapperReal = new ObjectMapper();
        JsonNode rootNode = mapperReal.readTree(jsonSimulado);

        given(restTemplateMock.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>(jsonSimulado, HttpStatus.OK));

        given(objectMapperMock.readTree(anyString())).willReturn(rootNode);

        // Educamos al mock para la serialización final a String
        given(objectMapperMock.writeValueAsString(any())).willReturn("[{\"letra\":\"A\"}]");

        // Ejecutamos
        String resultado = geminiService.generarRosco("Informática");

        // Verificaciones
        assertNotNull(resultado);
        assertTrue(resultado.startsWith("["));
        assertNotEquals("[]", resultado);
    }

    @Test
    void generarRosco_RamasErrorAPI() throws Exception {
        // Caso 1: Status no exitoso
        given(restTemplateMock.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST));
        assertEquals("[]", geminiService.generarRosco("Tema"));

        // Caso 2: JSON con campo "error"
        JsonNode errorNode = new ObjectMapper().readTree("{\"error\":\"quota exceeded\"}");
        given(restTemplateMock.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>("{}", HttpStatus.OK));
        given(objectMapperMock.readTree(anyString())).willReturn(errorNode);
        assertEquals("[]", geminiService.generarRosco("Tema"));

        // Caso 3: Candidates vacío
        JsonNode emptyNode = new ObjectMapper().readTree("{\"candidates\":[]}");
        given(objectMapperMock.readTree(anyString())).willReturn(emptyNode);
        assertEquals("[]", geminiService.generarRosco("Tema"));
    }

    @Test
    void generarRosco_CatchException() {
        given(restTemplateMock.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willThrow(new RuntimeException("Error fatal"));
        assertEquals("[]", geminiService.generarRosco("Tema"));
    }

    @Test
    void convertirTextoAJson_ErrorSerializacion() throws Exception {
        // Este test cubre la rama del catch dentro de convertirTextoAJson
        String textoIA = "A|Abeja|Definición A";
        JsonNode rootNode = new ObjectMapper().readTree("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + textoIA + "\"}]}}]}");

        given(restTemplateMock.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>("{}", HttpStatus.OK));
        given(objectMapperMock.readTree(anyString())).willReturn(rootNode);

        // Forzamos el catch al escribir el String final
        given(objectMapperMock.writeValueAsString(any())).willThrow(new RuntimeException("Error Jackson"));

        String resultado = geminiService.generarRosco("Tema");
        assertEquals("[]", resultado);
    }

    @Test
    void contarLineas_CatchException() throws Exception {
        // Cubre la rama del catch en el método contarLineas
        String textoIA = "A|Abeja|Definición A";
        JsonNode rootNode = new ObjectMapper().readTree("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + textoIA + "\"}]}}]}");

        given(restTemplateMock.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>("{}", HttpStatus.OK));

        // Primer readTree para obtener el texto (éxito)
        given(objectMapperMock.readTree(anyString()))
                .willReturn(rootNode) // Para extraer el texto
                .willThrow(new RuntimeException("Error en conteo")); // Para el método contarLineas()

        String resultado = geminiService.generarRosco("Tema");
        assertNotNull(resultado);
    }
}
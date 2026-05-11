package com.jorge.usuarios;

import com.jorge.usuarios.services.impl.EmailServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @InjectMocks
    private EmailServiceImpl emailService;

    @Test
    void generarPlantillaHtml() {
        String html = emailService.generarPlantillaHtml("PilotoPrueba");
        assertNotNull(html);
        assertTrue(html.contains("PilotoPrueba"));
        assertTrue(html.contains("FASTFINGERS"));
    }

    @Test
    void enviarCorreoBienvenida_Exito() {
        // Interceptamos la creación del 'new RestTemplate()' dentro del código
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    // Simulamos que Brevo responde con un 200 OK
                    ResponseEntity<String> responseEntity = new ResponseEntity<>("OK", HttpStatus.OK);

                    when(mock.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                            .thenReturn(responseEntity);
                })) {

            // Ejecutamos el método
            emailService.enviarCorreoBienvenida("test@test.com", "Paco");

            // Al devolver un 200 OK simulado, pasará por el 'if (is2xxSuccessful())'
            // y cubrirá la rama de éxito (el System.out.println)
        }
    }

    @Test
    void enviarCorreoBienvenida_Fallo() {
        // Interceptamos nuevamente el 'new RestTemplate()'
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    // Simulamos que la API de Brevo explota o no hay internet
                    when(mock.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                            .thenThrow(new RuntimeException("Fallo de red simulado"));
                })) {

            // Ejecutamos el método
            emailService.enviarCorreoBienvenida("test@test.com", "Paco");

            // Al lanzar excepción, pasará por el 'catch (Exception e)'
            // y cubrirá la rama de error (el System.err.println)
        }
    }
}
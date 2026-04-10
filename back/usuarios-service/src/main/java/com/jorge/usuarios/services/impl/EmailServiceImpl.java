package com.jorge.usuarios.services.impl;
import com.jorge.usuarios.services.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Override
    @Async
    public void enviarCorreoBienvenida(String emailDestino, String nombrePiloto) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.brevo.com/v3/smtp/email";

        // 1. Preparamos las aduanas (Headers)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        // 2. Construimos el paquete (Body) usando Maps para evitar errores de comillas
        Map<String, Object> body = new HashMap<>();
        body.put("sender", Map.of("name", "FastFingers", "email", "soportefastfingers@gmail.com"));
        body.put("to", List.of(Map.of("email", emailDestino, "name", nombrePiloto)));
        body.put("subject", "🏁 ¡Bienvenido a la pista, " + nombrePiloto + "!");
        body.put("htmlContent", generarPlantillaHtml(nombrePiloto));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            // 3. ¡Disparamos por el puerto 443!
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("🚀 [BREVO API] ¡Correo entregado con éxito a " + emailDestino + "!");
            }
        } catch (Exception e) {
            System.err.println("💥 [BREVO API] El envío falló: " + e.getMessage());
        }
    }
    @Override
    public String generarPlantillaHtml(String nombre) {
        return "<div style='font-family: Arial, sans-serif; background-color: #050b14; color: #c5c6c7; padding: 40px; text-align: center; border-radius: 10px;'>"
                + "<h1 style='color: #00f2fe;'>🏁 FASTFINGERS 🏁</h1>"
                + "<h2>¡Licencia de piloto confirmada, " + nombre + "!</h2>"
                + "<p>El semáforo está a punto de ponerse en verde. Calienta esos dedos.</p>"
                + "</div>";
    }
}
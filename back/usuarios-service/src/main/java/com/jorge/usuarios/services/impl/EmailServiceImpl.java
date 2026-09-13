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
        String logoUrl = "https://tfg-jbr2.onrender.com/static/images/favicon.png"; // Sustituye por la URL real de tu logo
        String webUrl = "https://fastfingers-tfg.railway.app"; // La URL de tu frontend en Railway

        return "<div style='background-color: #0d1117; color: #ffffff; font-family: \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; padding: 20px;'>"
                + "  <div style='max-width: 600px; margin: 0 auto; background: #161b22; border: 2px solid #00f2fe; border-radius: 15px; overflow: hidden; box-shadow: 0 0 20px rgba(0, 242, 254, 0.3);'>"

                // --- HEADER CON LOGO ---
                + "    <div style='padding: 30px; text-align: center; background: linear-gradient(135deg, #0d1117 0%, #161b22 100%);'>"
                + "      <img src='" + logoUrl + "' alt='FastFingers Logo' style='width: 150px; margin-bottom: 10px;'>"
                + "      <h1 style='margin: 0; font-size: 28px; letter-spacing: 3px; color: #00f2fe; text-shadow: 0 0 10px #00f2fe;'>FASTFINGERS</h1>"
                + "    </div>"

                // --- CUERPO ---
                + "    <div style='padding: 30px;'>"
                + "      <h2 style='color: #ffffff; border-bottom: 1px solid #30363d; padding-bottom: 10px;'>¡Bienvenido a la parrilla de salida, <span style='color: #00f2fe;'>" + nombre + "</span>!</h2>"
                + "      <p style='color: #8b949e; line-height: 1.6;'>Has completado tu registro con éxito. <b>FastFingers</b> no es solo un test de velocidad; es la arena donde los mecanógrafos más rápidos del mundo compiten por la gloria.</p>"

                // --- RESUMEN MODOS ---
                + "      <div style='background: #0d1117; padding: 20px; border-radius: 10px; margin: 20px 0;'>"
                + "        <h3 style='margin-top: 0; color: #00f2fe; font-size: 16px;'>🏎️ MODOS DE JUEGO:</h3>"
                + "        <ul style='list-style: none; padding: 0; color: #c9d1d9; font-size: 14px;'>"
                + "          <li style='margin-bottom: 8px;'>⏱️ <b>Contrarreloj:</b> ¿Cuántas frases puedes escribir en 60 segundos?</li>"
                + "          <li style='margin-bottom: 8px;'>🎯 <b>El Rosco:</b> Escribe un tema, la IA generará un rosco temático exclusivo para tí.</li>"
                + "          <li style='margin-bottom: 8px;'>\uD83E\uDDE0 <b>Contrarreloj a medida:</b> Escribe un tema, la IA generará frases sobre ese tema para tí.</li>"
                + "        </ul>"
                + "      </div>"

                // --- BOTÓN ---
                + "      <div style='text-align: center; margin-top: 40px;'>"
                + "        <a href='" + webUrl + "' style='background: #00f2fe; color: #0d1117; padding: 15px 35px; text-decoration: none; font-weight: bold; border-radius: 50px; text-transform: uppercase; box-shadow: 0 4px 15px rgba(0, 242, 254, 0.4); display: inline-block;'>Entrar en la Pista</a>"
                + "      </div>"
                + "    </div>"

                // --- FOOTER ---
                + "    <div style='background: #0d1117; padding: 20px; text-align: center; font-size: 12px; color: #484f58; border-top: 1px solid #30363d;'>"
                + "      <p>Estás recibiendo este correo porque te has registrado en FastFingers TFG.<br>© 2026 FastFingers Team</p>"
                + "    </div>"
                + "  </div>"
                + "</div>";
    }
}
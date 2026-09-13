package com.jorge.usuarios.services;

import org.springframework.scheduling.annotation.Async;

public interface EmailService {
    // Usamos @Async para que el envío del correo vaya por otro hilo
    // y el usuario no tenga que esperar a que se envíe para ver la pantalla de éxito.
    @Async
    void enviarCorreoBienvenida(String emailDestino, String nombrePiloto);

    // 🎨 Estética Racing Elite en HTML
    default String generarPlantillaHtml(String nombre) {
        return "<div style='font-family: Arial, sans-serif; background-color: #050b14; color: #c5c6c7; padding: 40px; text-align: center; border-radius: 10px; max-width: 600px; margin: auto; border: 2px solid rgba(0, 242, 254, 0.2);'>"
                + "<h1 style='color: #00f2fe; text-transform: uppercase; letter-spacing: 2px; text-shadow: 0 0 10px #00f2fe;'>🏁 FASTFINGERS 🏁</h1>"
                + "<h2 style='color: #ffffff;'>¡Licencia de piloto confirmada, " + nombre + "!</h2>"
                + "<p style='font-size: 16px; line-height: 1.6;'>Tu registro se ha completado con éxito. Ya tienes acceso oficial a los boxes, al mercado de power-ups y, por supuesto, a la pista.</p>"
                + "<div style='background-color: rgba(31, 40, 51, 0.85); padding: 20px; border-radius: 8px; margin: 25px 0; border-left: 4px solid #00f2fe;'>"
                + "  <h3 style='color: #00f2fe; margin-top: 0;'>🔧 Primeros Pasos:</h3>"
                + "  <p style='margin: 5px 0;'>1. Inicia sesión con tus credenciales.</p>"
                + "  <p style='margin: 5px 0;'>2. Juega tu primera partida y gana créditos.</p>"
                + "  <p style='margin: 5px 0;'>3. Visita la tienda para equipar tu vehículo.</p>"
                + "</div>"
                + "<p style='color: #8892b0; font-size: 14px;'>Calienta esos dedos... el semáforo está a punto de ponerse en verde.</p>"
                + "</div>";
    }
}

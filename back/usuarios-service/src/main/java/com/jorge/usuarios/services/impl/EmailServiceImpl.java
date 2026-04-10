package com.jorge.usuarios.services.impl;

import com.jorge.usuarios.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Usamos @Async para que el envío del correo vaya por otro hilo
    // y el usuario no tenga que esperar a que se envíe para ver la pantalla de éxito.
    @Async
    @Override
    public void enviarCorreoBienvenida(String emailDestino, String nombrePiloto) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(emailDestino);
            helper.setSubject("🏁 ¡Bienvenido a la pista, " + nombrePiloto + "!");
            helper.setText(generarPlantillaHtml(nombrePiloto), true); // El 'true' activa el modo HTML

            mailSender.send(mensaje);
            System.out.println("📻 [EMAIL] Correo de bienvenida enviado a boxes: " + emailDestino);

        } catch (MessagingException e) {
            System.err.println("💥 [EMAIL] Fallo en las comunicaciones: " + e.getMessage());
        }
    }

}
package com.jorge.usuarios;

import com.jorge.usuarios.services.EmailService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailServiceTest {

    @Test
    void testGenerarPlantillaHtml() {
        // 1. Creamos una implementación anónima de la interfaz solo para el test
        EmailService emailService = new EmailService() {
            @Override
            public void enviarCorreoBienvenida(String emailDestino, String nombrePiloto) {
                // No necesitamos implementar esto aquí,
                // solo queremos probar el "default"
            }
        };

        // 2. Llamamos al default directamente desde la interfaz
        String html = emailService.generarPlantillaHtml("Piloto de Pruebas");

        // 3. Verificamos que pasa por todas las líneas del return
        assertNotNull(html);
        assertTrue(html.contains("Piloto de Pruebas"));
        assertTrue(html.contains("FASTFINGERS"));
        assertTrue(html.contains("¡Licencia de piloto confirmada"));
    }
}
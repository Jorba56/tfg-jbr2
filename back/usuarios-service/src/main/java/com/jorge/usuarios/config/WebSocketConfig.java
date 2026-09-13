package com.jorge.usuarios.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // El prefijo para los mensajes que el servidor ENVÍA a los clientes (Ej: a la sala de juego)
        config.enableSimpleBroker("/topic");
        // El prefijo para los mensajes que los clientes ENVÍAN al servidor
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // La URL a la que se conectará el JavaScript para abrir el túnel
        registry.addEndpoint("/ws-game")
                .setAllowedOriginPatterns("*") // Permite que cualquier frontend se conecte
                .withSockJS(); // Fallback de seguridad por si algún navegador antiguo falla
    }
}
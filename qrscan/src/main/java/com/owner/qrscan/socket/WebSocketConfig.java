package com.owner.qrscan.socket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Bean
    public SocketConnectionHandler socketConnectionHandler() {
        System.out.println("🔧 Creando bean SocketConnectionHandler...");
        return new SocketConnectionHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        System.out.println("🚀 Iniciando registro de WebSocket handlers...");

        try {
            // Endpoint principal con SockJS
            registry
                    .addHandler(socketConnectionHandler(), "/hello")
                    .setAllowedOrigins("*") // Para desarrollo
                    .withSockJS();
            System.out.println("✅ Handler registrado: /hello (con SockJS)");

            // Endpoint WebSocket directo
            registry
                    .addHandler(socketConnectionHandler(), "/ws")
                    .setAllowedOrigins("*");
            System.out.println("✅ Handler registrado: /ws (directo)");

            // Endpoint adicional para pruebas
            registry
                    .addHandler(socketConnectionHandler(), "/websocket")
                    .setAllowedOrigins("*");
            System.out.println("✅ Handler registrado: /websocket (directo)");

            System.out.println("🎉 Todos los WebSocket handlers registrados exitosamente!");

        } catch (Exception e) {
            System.err.println("❌ Error registrando WebSocket handlers: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
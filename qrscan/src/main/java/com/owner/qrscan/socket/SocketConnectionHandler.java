package com.owner.qrscan.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Component
public class SocketConnectionHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(SocketConnectionHandler.class);

    // Usar ConcurrentHashMap para mejor rendimiento y thread safety
    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        String sessionId = session.getId();
        sessions.put(sessionId, session);

        logger.info("🔗 Cliente conectado: {} (Total conectados: {})", sessionId, sessions.size());

        // Enviar mensaje de bienvenida al cliente que se acaba de conectar
        try {
            session.sendMessage(new TextMessage("¡Conectado exitosamente! ID: " + sessionId));
        } catch (IOException e) {
            logger.error("Error enviando mensaje de bienvenida", e);
        }

        // Notificar a todos los demás clientes sobre la nueva conexión
        broadcastMessage("Un nuevo cliente se ha conectado. Total: " + sessions.size(), sessionId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        String sessionId = session.getId();
        sessions.remove(sessionId);

        logger.info("❌ Cliente desconectado: {} - Razón: {} (Total conectados: {})",
                sessionId, status.toString(), sessions.size());

        // Notificar a todos los clientes sobre la desconexión
        broadcastMessage("Un cliente se ha desconectado. Total: " + sessions.size(), null);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        super.handleMessage(session, message);

        String sessionId = session.getId();
        String messageContent = message.getPayload().toString();

        logger.info("📨 Mensaje recibido de {}: {}", sessionId, messageContent);

        // Crear mensaje con información del remitente
        String formattedMessage = String.format("[%s]: %s", sessionId.substring(0, 8), messageContent);

        // Enviar a todos excepto al remitente
        broadcastMessage(formattedMessage, sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);

        String sessionId = session.getId();
        logger.error("⚠️ Error de transporte en sesión {}: {}", sessionId, exception.getMessage());

        // Limpiar la sesión si hay error
        sessions.remove(sessionId);
    }


    /**
     * Envía un mensaje a todas las sesiones activas excepto a la excluida
     */
    private void broadcastMessage(String message, String excludeSessionId) {
        TextMessage textMessage = new TextMessage(message);

        sessions.values().parallelStream().forEach(session -> {
            if (excludeSessionId != null && session.getId().equals(excludeSessionId)) {
                return; // Omitir la sesión excluida
            }

            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    logger.error("Error enviando mensaje a sesión {}: {}", session.getId(), e.getMessage());
                    // Remover sesión problemática
                    sessions.remove(session.getId());
                }
            } else {
                // Limpiar sesiones cerradas
                sessions.remove(session.getId());
            }
        });

        logger.info("📡 Mensaje enviado a {} clientes", sessions.size() - (excludeSessionId != null ? 1 : 0));
    }

    /**
     * Obtiene el número de clientes conectados
     */
    public int getConnectedClientsCount() {
        return sessions.size();
    }

    public boolean sendMessageToClient(String sessionId, String message) {
        WebSocketSession session = sessions.get(sessionId);

        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(message));
                }
                logger.info("📤 Mensaje enviado al cliente {}: {}", sessionId, message);
                return true;
            } catch (IOException e) {
                logger.error("❌ Error al enviar mensaje al cliente {}: {}", sessionId, e.getMessage());
            }
        } else {
            logger.warn("⚠️ Sesión no encontrada o cerrada para el cliente: {}", sessionId);
        }

        return false;
    }
}
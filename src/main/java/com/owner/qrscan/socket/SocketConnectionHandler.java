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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class SocketConnectionHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(SocketConnectionHandler.class);

    // Usar ConcurrentHashMap para mejor rendimiento y thread safety
    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // Executor para manejar operaciones asíncronas de manera controlada
    private final ExecutorService broadcastExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "websocket-broadcast");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        String sessionId = session.getId();

        // Extraer socketId de los parámetros de query
        Map<String, String> params = getQueryParams(session.getUri().getQuery());
        String socketId = params.get("socketid");

        // Agregar la sesión usando el ID de sesión como clave principal
        sessions.put(sessionId, session);

        // Si hay socketId, también indexar por él (para búsquedas por socketId)
        if (socketId != null) {
            sessions.put(socketId, session);
            logger.info("✅ Cliente conectado con socketId: {} (sessionId: {})", socketId, sessionId);
        } else {
            logger.info("✅ Cliente conectado sin socketId (sessionId: {})", sessionId);
        }

        logger.info("🔗 Total de clientes conectados: {}", getUniqueSessionCount());

        // Enviar mensaje de bienvenida de manera segura
        sendSafeMessage(session, "¡Conectado exitosamente! ID: " + sessionId);

        // Notificar a otros clientes de manera asíncrona para evitar bloqueos
        CompletableFuture.runAsync(() -> {
            broadcastMessage("Un nuevo cliente se ha conectado. Total: " + getUniqueSessionCount(), sessionId);
        }, broadcastExecutor);
    }

    private Map<String, String> getQueryParams(String query) {
        Map<String, String> map = new ConcurrentHashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] parts = param.split("=");
                if (parts.length == 2) {
                    map.put(parts[0], parts[1]);
                }
            }
        }
        return map;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        String sessionId = session.getId();

        // Remover la sesión ANTES de hacer broadcast para evitar race conditions
        removeSession(sessionId);

        logger.info("❌ Cliente desconectado: {} - Razón: {} (Total conectados: {})",
                sessionId, status.toString(), getUniqueSessionCount());

        // Hacer broadcast de manera asíncrona y después de limpiar
        CompletableFuture.runAsync(() -> {
            broadcastMessage("Un cliente se ha desconectado. Total: " + getUniqueSessionCount(), null);
        }, broadcastExecutor);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        super.handleMessage(session, message);

        String sessionId = session.getId();
        String messageContent = message.getPayload().toString();

        logger.info("📨 Mensaje recibido de {}: {}", sessionId, messageContent);

        // Crear mensaje con información del remitente
        String formattedMessage = String.format("[%s]: %s", sessionId.substring(0, 8), messageContent);

        // Enviar a todos excepto al remitente de manera asíncrona
        CompletableFuture.runAsync(() -> {
            broadcastMessage(formattedMessage, sessionId);
        }, broadcastExecutor);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);

        String sessionId = session.getId();
        logger.error("⚠️ Error de transporte en sesión {}: {}", sessionId, exception.getMessage());

        // Limpiar la sesión si hay error
        removeSession(sessionId);
    }

    /**
     * Remueve una sesión de manera segura
     */
    private void removeSession(String sessionId) {
        WebSocketSession removedSession = sessions.remove(sessionId);

        // También buscar y remover por socketId si existe
        sessions.entrySet().removeIf(entry -> {
            WebSocketSession session = entry.getValue();
            return session != null && sessionId.equals(session.getId());
        });

        if (removedSession != null) {
            logger.debug("🧹 Sesión {} removida del mapa", sessionId);
        }
    }

    /**
     * Envía un mensaje de manera segura a una sesión específica
     */
    private boolean sendSafeMessage(WebSocketSession session, String message) {
        if (session == null) {
            return false;
        }

        try {
            // Verificar que la sesión esté abierta antes de enviar
            if (session.isOpen()) {
                synchronized (session) {
                    // Doble verificación dentro del synchronized
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error enviando mensaje a sesión {}: {}", session.getId(), e.getMessage());
            // Limpiar sesión problemática
            removeSession(session.getId());
        }

        return false;
    }

    /**
     * Envía un mensaje a todas las sesiones activas excepto a la excluida
     * Versión mejorada que evita race conditions
     */
    private void broadcastMessage(String message, String excludeSessionId) {
        if (message == null) {
            return;
        }

        // Limpiar sesiones muertas antes de hacer broadcast
        cleanupClosedSessions();

        // Obtener snapshot de sesiones activas
        Map<String, WebSocketSession> activeSessionsSnapshot = new ConcurrentHashMap<>();

        sessions.forEach((key, session) -> {
            if (session != null && session.isOpen() &&
                    (excludeSessionId == null || !session.getId().equals(excludeSessionId))) {
                activeSessionsSnapshot.put(key, session);
            }
        });

        // Enviar mensajes de manera secuencial para evitar problemas de concurrencia
        int successCount = 0;
        for (WebSocketSession session : activeSessionsSnapshot.values()) {
            if (sendSafeMessage(session, message)) {
                successCount++;
            }
        }

        logger.info("📡 Mensaje enviado exitosamente a {} de {} clientes",
                successCount, activeSessionsSnapshot.size());
    }

    /**
     * Limpia sesiones cerradas del mapa
     */
    private void cleanupClosedSessions() {
        sessions.entrySet().removeIf(entry -> {
            WebSocketSession session = entry.getValue();
            boolean shouldRemove = session == null || !session.isOpen();
            if (shouldRemove) {
                logger.debug("🧹 Limpiando sesión cerrada: {}", entry.getKey());
            }
            return shouldRemove;
        });
    }

    /**
     * Obtiene el número real de sesiones únicas conectadas
     */
    private int getUniqueSessionCount() {
        cleanupClosedSessions();
        return (int) sessions.values().stream()
                .filter(session -> session != null && session.isOpen())
                .map(WebSocketSession::getId)
                .distinct()
                .count();
    }

    /**
     * Obtiene el número de clientes conectados (API pública)
     */
    public int getConnectedClientsCount() {
        return getUniqueSessionCount();
    }

    /**
     * Envía un mensaje a un cliente específico de manera segura
     */
    public boolean sendMessageToClient(String sessionId, String message) {
        if (sessionId == null || message == null) {
            return false;
        }

        WebSocketSession session = sessions.get(sessionId);

        if (session != null) {
            boolean success = sendSafeMessage(session, message);
            if (success) {
                logger.info("📤 Mensaje enviado al cliente {}: {}", sessionId, message);
            } else {
                logger.warn("⚠️ No se pudo enviar mensaje al cliente: {}", sessionId);
            }
            return success;
        } else {
            logger.warn("⚠️ Sesión no encontrada para el cliente: {}", sessionId);
            return false;
        }
    }

    /**
     * Método para limpiar recursos al cerrar la aplicación
     */
    public void shutdown() {
        logger.info("🔄 Cerrando executor de broadcast...");
        broadcastExecutor.shutdown();

        // Cerrar todas las sesiones activas
        sessions.values().forEach(session -> {
            if (session != null && session.isOpen()) {
                try {
                    session.close();
                } catch (Exception e) {
                    logger.error("Error cerrando sesión {}: {}", session.getId(), e.getMessage());
                }
            }
        });

        sessions.clear();
        logger.info("✅ Recursos WebSocket liberados");
    }
}
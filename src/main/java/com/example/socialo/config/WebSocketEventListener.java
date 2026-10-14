package com.example.socialo.config;

import com.example.socialo.controller.ChatWebSocketController;
import com.example.socialo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/** Updates lastActiveAt and broadcasts presence on connect/disconnect. */
@Component
public class WebSocketEventListener {

    @Autowired private UserService userService;
    @Autowired private ChatWebSocketController chatController;

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        String name = event.getUser() != null ? event.getUser().getName() : null;
        if (name == null) return;
        try {
            Long userId = Long.parseLong(name);
            userService.touchLastActive(userId);
            chatController.broadcastPresence(userId, true);
        } catch (NumberFormatException ignored) {}
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String name = event.getUser() != null ? event.getUser().getName() : null;
        if (name == null) return;
        try {
            Long userId = Long.parseLong(name);
            userService.touchLastActive(userId);
            chatController.broadcastPresence(userId, false);
        } catch (NumberFormatException ignored) {}
    }
}

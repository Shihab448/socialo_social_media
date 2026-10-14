package com.example.socialo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Map;

/**
 * STOMP over WebSocket configuration.
 * Endpoint: /ws  (SockJS fallback allowed).
 * Per-user destinations: /user/queue/*
 * App destinations (sent from browser): /app/*
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker for per-user queues and broadcast topic
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(new SessionHandshakeInterceptor())
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * Copies userId from the HTTP session into the WebSocket session attributes,
     * so we can authenticate the WebSocket connection.
     */
    static class SessionHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(
                org.springframework.http.server.ServerHttpRequest request,
                org.springframework.http.server.ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Map<String, Object> attributes) {

            if (request instanceof org.springframework.http.server.ServletServerHttpRequest servletReq) {
                HttpSession session = servletReq.getServletRequest().getSession(false);
                if (session != null && session.getAttribute("userId") != null) {
                    attributes.put("userId", session.getAttribute("userId"));
                    return true;
                }
            }
            return false; // reject if not authenticated
        }

        @Override
        public void afterHandshake(
                org.springframework.http.server.ServerHttpRequest request,
                org.springframework.http.server.ServerHttpResponse response,
                WebSocketHandler wsHandler, Exception exception) {}
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    Object userId = accessor.getSessionAttributes() != null
                            ? accessor.getSessionAttributes().get("userId") : null;
                    if (userId != null) {
                        // Set Principal so Spring can route to /user/{userId}/queue/*
                        accessor.setUser(new UserPrincipal(userId.toString()));
                    }
                }
                return message;
            }
        });
    }

    /** Simple Principal wrapper around userId string. */
    public record UserPrincipal(String name) implements Principal {
        @Override public String getName() { return name; }
    }
}

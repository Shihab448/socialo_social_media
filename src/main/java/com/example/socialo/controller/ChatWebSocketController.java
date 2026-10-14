package com.example.socialo.controller;

import com.example.socialo.model.*;
import com.example.socialo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Handles all real-time STOMP messages over WebSocket:
 *   /app/chat.send      — send a text message in a conversation
 *   /app/chat.typing    — typing indicator
 *   /app/chat.seen      — mark messages as read
 *   /app/chat.react     — emoji reaction on a message
 *   /app/chat.unsend    — soft-delete a message
 *   /app/call.signal    — WebRTC call signaling relay
 */
@Controller
public class ChatWebSocketController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired private SimpMessagingTemplate broker;
    @Autowired private MessageService messageService;
    @Autowired private UserService userService;

    // ── Send text message ─────────────────────────────────────

    @MessageMapping("/chat.send")
    public void send(Principal principal, @Payload Map<String, Object> payload) {
        Long userId = Long.parseLong(principal.getName());
        User sender = userService.getById(userId);

        Long convId    = toLong(payload.get("conversationId"));
        String content = (String) payload.getOrDefault("content", "");
        Long replyToId = payload.containsKey("replyToId") ? toLong(payload.get("replyToId")) : null;

        Conversation conv = messageService.findById(convId).orElse(null);
        if (conv == null || !messageService.isMember(conv, sender)) return;

        userService.touchLastActive(userId);
        Message msg = messageService.sendTextMessage(conv, sender, content, replyToId);

        Map<String, Object> out = buildPayload(msg, sender);
        conv.getMembers().forEach(cm ->
            broker.convertAndSendToUser(cm.getUser().getId().toString(), "/queue/messages", out));
    }

    // ── Typing indicator ──────────────────────────────────────

    @MessageMapping("/chat.typing")
    public void typing(Principal principal, @Payload Map<String, Object> payload) {
        Long userId  = Long.parseLong(principal.getName());
        User sender  = userService.getById(userId);
        Long convId  = toLong(payload.get("conversationId"));
        boolean isTyping = Boolean.TRUE.equals(payload.get("typing"));

        Conversation conv = messageService.findById(convId).orElse(null);
        if (conv == null || !messageService.isMember(conv, sender)) return;

        Map<String, Object> out = Map.of(
            "conversationId", convId,
            "userId", userId,
            "name", sender.getFirstName(),
            "typing", isTyping
        );
        conv.getMembers().stream()
            .filter(cm -> !cm.getUser().getId().equals(userId))
            .forEach(cm -> broker.convertAndSendToUser(
                cm.getUser().getId().toString(), "/queue/typing", out));
    }

    // ── Mark messages as seen ─────────────────────────────────

    @MessageMapping("/chat.seen")
    public void seen(Principal principal, @Payload Map<String, Object> payload) {
        Long userId    = Long.parseLong(principal.getName());
        User user      = userService.getById(userId);
        Long convId    = toLong(payload.get("conversationId"));
        Long lastMsgId = toLong(payload.get("lastMessageId"));

        Conversation conv = messageService.findById(convId).orElse(null);
        if (conv == null) return;

        messageService.markRead(conv, user, lastMsgId);

        Map<String, Object> receipt = Map.of(
            "conversationId", convId,
            "seenBy", userId,
            "lastMessageId", lastMsgId
        );
        conv.getMembers().stream()
            .filter(cm -> !cm.getUser().getId().equals(userId))
            .forEach(cm -> broker.convertAndSendToUser(
                cm.getUser().getId().toString(), "/queue/receipts", receipt));
    }

    // ── Emoji reaction on a message ───────────────────────────

    @MessageMapping("/chat.react")
    public void react(Principal principal, @Payload Map<String, Object> payload) {
        Long userId   = Long.parseLong(principal.getName());
        User user     = userService.getById(userId);
        Long msgId    = toLong(payload.get("messageId"));
        String emoji  = (String) payload.getOrDefault("emoji", "👍");

        Message msg = messageService.findMessageById(msgId).orElse(null);
        if (msg == null) return;

        Conversation conv = msg.getConversation();
        if (!messageService.isMember(conv, user)) return;

        messageService.toggleReaction(msgId, user, emoji);

        List<MessageReaction> reactions = messageService.getReactions(msgId);
        Map<String, Object> out = Map.of(
            "messageId", msgId,
            "reactions", reactions.stream().map(r -> Map.of(
                "userId", r.getUser().getId(),
                "name",   r.getUser().getFirstName(),
                "emoji",  r.getEmoji()
            )).toList()
        );
        conv.getMembers().forEach(cm ->
            broker.convertAndSendToUser(cm.getUser().getId().toString(), "/queue/messages", out));
    }

    // ── Unsend (soft-delete) ──────────────────────────────────

    @MessageMapping("/chat.unsend")
    public void unsend(Principal principal, @Payload Map<String, Object> payload) {
        Long userId = Long.parseLong(principal.getName());
        Long msgId  = toLong(payload.get("messageId"));

        Message msg = messageService.findMessageById(msgId).orElse(null);
        if (msg == null) return;

        Conversation conv = msg.getConversation();
        messageService.unsend(msgId, userId);

        Map<String, Object> out = Map.of("messageId", msgId, "deleted", true);
        conv.getMembers().forEach(cm ->
            broker.convertAndSendToUser(cm.getUser().getId().toString(), "/queue/messages", out));
    }

    // ── WebRTC call signaling relay ───────────────────────────

    /**
     * Relays any WebRTC signal to the target user.
     * Payload must include: targetUserId (Long), type (String), and any additional data.
     * Types: invite, accept, reject, busy, cancel, offer, answer, ice-candidate, end
     */
    @MessageMapping("/call.signal")
    public void callSignal(Principal principal, @Payload Map<String, Object> payload) {
        Long fromId  = Long.parseLong(principal.getName());
        User from    = userService.getById(fromId);
        Long targetId = toLong(payload.get("targetUserId"));

        Map<String, Object> out = new LinkedHashMap<>(payload);
        out.put("fromUserId", fromId);
        out.put("fromName",   from.getFullName());
        out.put("fromPhoto",  from.getDisplayProfileImage());

        broker.convertAndSendToUser(targetId.toString(), "/queue/call", out);
        userService.touchLastActive(fromId);
    }

    // ── Presence broadcast (called by WebSocket event listener) ──

    /** Broadcast online/offline status to all connected users. */
    public void broadcastPresence(Long userId, boolean online) {
        Map<String, Object> ev = Map.of("userId", userId, "online", online);
        broker.convertAndSend("/topic/presence", ev);
    }

    // ── Helpers ───────────────────────────────────────────────

    private Map<String, Object> buildPayload(Message msg, User sender) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id",            msg.getId());
        out.put("conversationId", msg.getConversation().getId());
        out.put("senderId",      sender.getId());
        out.put("senderName",    sender.getFullName());
        out.put("senderPhoto",   sender.getDisplayProfileImage());
        out.put("content",       msg.getContent());
        out.put("mediaUrl",      msg.getMediaUrl());
        out.put("mediaType",     msg.getMediaType().name());
        out.put("fileName",      msg.getFileName());
        out.put("fileSize",      msg.getFileSize());
        out.put("deleted",       msg.isDeleted());
        out.put("createdAt",     msg.getCreatedAt().format(FMT));
        if (msg.getReplyTo() != null) {
            out.put("replyTo", Map.of(
                "id",         msg.getReplyTo().getId(),
                "content",    msg.getReplyTo().getDisplayContent(),
                "senderName", msg.getReplyTo().getSender().getFirstName()
            ));
        }
        return out;
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }
}

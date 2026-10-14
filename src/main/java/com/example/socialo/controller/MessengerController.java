package com.example.socialo.controller;

import com.example.socialo.model.*;
import com.example.socialo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** REST + page controllers for the Messenger feature. Real-time is handled by ChatWebSocketController. */
@Controller
public class MessengerController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired private MessageService messageService;
    @Autowired private UserService userService;
    @Autowired private FriendService friendService;
    @Autowired private NotificationService notificationService;
    @Autowired private BlockService blockService;

    // ── Messenger home ────────────────────────────────────────

    @GetMapping("/chat")
    public String chatHome(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        List<Conversation> convs = messageService.getConversationsForUser(user);
        model.addAttribute("currentUser", user);
        model.addAttribute("conversations", convs);
        model.addAttribute("selectedConversation", null);
        model.addAttribute("totalUnread", messageService.getTotalUnread(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        return "chat";
    }

    // ── Open conversation ─────────────────────────────────────

    @GetMapping("/chat/{conversationId}")
    public String openChat(@PathVariable Long conversationId,
                           HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);

        Conversation conv = messageService.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        if (!messageService.isMember(conv, user)) return "redirect:/chat";

        List<Message> messages = messageService.getMessages(conv);
        User otherUser = conv.isGroup() ? null : messageService.getOtherUser(conv, user);

        // Compute per-conversation unread map
        Map<Long, Long> unreadMap = new HashMap<>();
        messageService.getConversationsForUser(user)
                      .forEach(c -> unreadMap.put(c.getId(), messageService.getUnreadCount(c, user)));

        // Is there a block between the DM participants?
        boolean chatBlocked = false;
        if (!conv.isGroup() && otherUser != null)
            chatBlocked = blockService.isBlockedEither(user, otherUser);

        model.addAttribute("currentUser", user);
        model.addAttribute("conversations", messageService.getConversationsForUser(user));
        model.addAttribute("selectedConversation", conv);
        model.addAttribute("messages", messages);
        model.addAttribute("otherUser", otherUser);
        model.addAttribute("chatBlocked", chatBlocked);
        model.addAttribute("unreadMap", unreadMap);
        model.addAttribute("totalUnread", messageService.getTotalUnread(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));

        // Mark read up to last message
        if (!messages.isEmpty())
            messageService.markRead(conv, user, messages.get(messages.size() - 1).getId());

        return "chat";
    }

    // ── Paginated older messages (AJAX) ───────────────────────

    @GetMapping("/chat/{conversationId}/messages")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> olderMessages(
            @PathVariable Long conversationId,
            @RequestParam Long before,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();

        User user = userService.getById(userId);
        Conversation conv = messageService.findById(conversationId).orElse(null);
        if (conv == null || !messageService.isMember(conv, user))
            return ResponseEntity.status(403).build();

        List<Map<String, Object>> result = messageService.getOlderMessages(conv, before)
                .stream().map(m -> buildMsgMap(m, userId)).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ── Start DM ──────────────────────────────────────────────

    @PostMapping("/chat/create")
    public String createDm(@RequestParam Long targetUserId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        User user   = userService.getById(userId);
        User target = userService.getById(targetUserId);

        if (blockService.isBlockedEither(user, target))
            return "redirect:/chat";

        Conversation conv = messageService.getOrCreateDm(user, target);
        return "redirect:/chat/" + conv.getId();
    }

    // ── Send message (fallback form POST – real-time via WS) ──

    @PostMapping("/chat/send")
    public String sendMessage(@RequestParam Long conversationId,
                              @RequestParam(required = false) String content,
                              @RequestParam(required = false) MultipartFile attachment,
                              @RequestParam(required = false) Long replyToId,
                              HttpSession session, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        Conversation conv = messageService.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        try {
            if (attachment != null && !attachment.isEmpty()) {
                messageService.sendMediaMessage(conv, user, attachment, content);
            } else if (content != null && !content.isBlank()) {
                messageService.sendTextMessage(conv, user, content, replyToId);
            }
            notifyOtherMembers(conv, user, conversationId);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/chat/" + conversationId;
    }

    // ── Group: create ─────────────────────────────────────────

    @GetMapping("/chat/group/create")
    public String createGroupPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        model.addAttribute("currentUser", user);
        model.addAttribute("friends", friendService.getFriends(user));
        return "chat-group-create";
    }

    @PostMapping("/chat/group/create")
    public String createGroup(@RequestParam String groupName,
                              @RequestParam List<Long> memberIds,
                              @RequestParam(required = false) MultipartFile groupPhoto,
                              HttpSession session, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        User creator = userService.getById(userId);
        List<User> members = memberIds.stream().map(userService::getById).toList();
        try {
            Conversation group = messageService.createGroup(groupName, creator, members);
            if (groupPhoto != null && !groupPhoto.isEmpty())
                messageService.updateGroupPhoto(group, creator, groupPhoto);
            return "redirect:/chat/" + group.getId();
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/chat";
        }
    }

    // ── Group: add member ─────────────────────────────────────

    @PostMapping("/chat/{convId}/group/add")
    public String addMember(@PathVariable Long convId,
                            @RequestParam Long memberId,
                            HttpSession session, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        User actor = userService.getById(userId);
        User newMember = userService.getById(memberId);
        Conversation conv = messageService.findById(convId)
                .orElseThrow(() -> new RuntimeException("Not found"));
        try { messageService.addGroupMember(conv, actor, newMember); }
        catch (RuntimeException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/chat/" + convId;
    }

    // ── Group: remove member ──────────────────────────────────

    @PostMapping("/chat/{convId}/group/remove")
    public String removeMember(@PathVariable Long convId,
                               @RequestParam Long memberId,
                               HttpSession session, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        User actor  = userService.getById(userId);
        User target = userService.getById(memberId);
        Conversation conv = messageService.findById(convId)
                .orElseThrow(() -> new RuntimeException("Not found"));
        try { messageService.removeGroupMember(conv, actor, target); }
        catch (RuntimeException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/chat/" + convId;
    }

    // ── Group: leave ──────────────────────────────────────────

    @PostMapping("/chat/{convId}/group/leave")
    public String leaveGroup(@PathVariable Long convId,
                             HttpSession session, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        Conversation conv = messageService.findById(convId)
                .orElseThrow(() -> new RuntimeException("Not found"));
        messageService.leaveGroup(conv, user);
        return "redirect:/chat";
    }

    // ── Group: rename ─────────────────────────────────────────

    @PostMapping("/chat/{convId}/group/rename")
    public String renameGroup(@PathVariable Long convId,
                              @RequestParam String groupName,
                              HttpSession session, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        User actor = userService.getById(userId);
        Conversation conv = messageService.findById(convId)
                .orElseThrow(() -> new RuntimeException("Not found"));
        try { messageService.renameGroup(conv, actor, groupName); }
        catch (RuntimeException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/chat/" + convId;
    }

    // ── Group: delete ─────────────────────────────────────────

    @PostMapping("/chat/{convId}/group/delete")
    public String deleteGroup(@PathVariable Long convId,
                              HttpSession session, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        User actor = userService.getById(userId);
        Conversation conv = messageService.findById(convId)
                .orElseThrow(() -> new RuntimeException("Not found"));
        try { messageService.deleteGroup(conv, actor); }
        catch (RuntimeException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/chat";
    }

    // ── Helpers ───────────────────────────────────────────────

    private void notifyOtherMembers(Conversation conv, User sender, Long convId) {
        conv.getMembers().stream()
            .map(ConversationMember::getUser)
            .filter(u -> !u.getId().equals(sender.getId()))
            .forEach(recipient -> notificationService.create(
                recipient, sender, "MESSAGE", convId,
                sender.getFullName() + " sent you a message."));
    }

    private Map<String, Object> buildMsgMap(Message m, Long currentUserId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id",        m.getId());
        map.put("senderId",  m.getSender().getId());
        map.put("senderName",m.getSender().getFullName());
        map.put("senderPhoto",m.getSender().getDisplayProfileImage());
        map.put("content",   m.getDisplayContent());
        map.put("mediaUrl",  m.getMediaUrl());
        map.put("mediaType", m.getMediaType().name());
        map.put("fileName",  m.getFileName());
        map.put("fileSize",  m.getFileSize());
        map.put("deleted",   m.isDeleted());
        map.put("mine",      m.getSender().getId().equals(currentUserId));
        map.put("createdAt", m.getCreatedAt().format(FMT));
        return map;
    }
}

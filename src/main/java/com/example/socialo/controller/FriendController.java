package com.example.socialo.controller;

import com.example.socialo.model.FriendRequest;
import com.example.socialo.model.User;
import com.example.socialo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class FriendController {

    @Autowired private FriendService friendService;
    @Autowired private UserService userService;
    @Autowired private NotificationService notificationService;

    // ── Friends Page ──────────────────────────────────────────

    @GetMapping("/friends")
    public String friendsPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);

        List<User> friends = friendService.getFriends(user);
        List<FriendRequest> pendingRequests = friendService.getPendingRequests(user);
        List<FriendRequest> sentRequests = friendService.getSentRequests(user);
        List<User> suggestions = friendService.getSuggestions(user);

        model.addAttribute("currentUser", user);
        model.addAttribute("friends", friends);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("sentRequests", sentRequests);
        model.addAttribute("suggestions", suggestions);
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));

        return "friends";
    }

    // ── Send Request ──────────────────────────────────────────

    @PostMapping("/friends/request/{targetId}")
    public String sendRequest(@PathVariable Long targetId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        User sender = userService.getById(userId);
        User receiver = userService.getById(targetId);
        try {
            friendService.sendRequest(sender, receiver);
            redirectAttributes.addFlashAttribute("success", "Friend request sent!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/user-profile/" + targetId;
    }

    // ── Accept Request ────────────────────────────────────────

    @PostMapping("/friends/accept/{requestId}")
    public String acceptRequest(@PathVariable Long requestId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        try {
            friendService.acceptRequest(requestId, user);
            redirectAttributes.addFlashAttribute("success", "Friend request accepted!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/friends";
    }

    // ── Reject Request ────────────────────────────────────────

    @PostMapping("/friends/reject/{requestId}")
    public String rejectRequest(@PathVariable Long requestId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        try {
            friendService.rejectRequest(requestId, user);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/friends";
    }

    // ── Cancel Sent Request ───────────────────────────────────

    @PostMapping("/friends/cancel/{targetId}")
    public String cancelRequest(@PathVariable Long targetId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        User sender = userService.getById(userId);
        User receiver = userService.getById(targetId);
        friendService.cancelRequest(sender, receiver);
        return "redirect:/user-profile/" + targetId;
    }

    // ── Remove Friend ─────────────────────────────────────────

    @PostMapping("/friends/remove/{friendId}")
    public String removeFriend(@PathVariable Long friendId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        User friend = userService.getById(friendId);
        friendService.removeFriend(user, friend);
        redirectAttributes.addFlashAttribute("success", "Friend removed.");
        return "redirect:/friends";
    }

    // ── AJAX: Send friend request (returns JSON) ──────────────

    @PostMapping("/friends/request-ajax/{targetId}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>>
    sendRequestAjax(@PathVariable Long targetId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return org.springframework.http.ResponseEntity.status(401).build();
        User sender   = userService.getById(userId);
        User receiver = userService.getById(targetId);
        try {
            friendService.sendRequest(sender, receiver);
            return org.springframework.http.ResponseEntity.ok(
                java.util.Map.of("status", "REQUEST_SENT"));
        } catch (RuntimeException e) {
            return org.springframework.http.ResponseEntity.ok(
                java.util.Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    // ── AJAX: Cancel sent request (returns JSON) ──────────────

    @PostMapping("/friends/cancel-ajax/{targetId}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>>
    cancelRequestAjax(@PathVariable Long targetId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return org.springframework.http.ResponseEntity.status(401).build();
        User sender   = userService.getById(userId);
        User receiver = userService.getById(targetId);
        friendService.cancelRequest(sender, receiver);
        return org.springframework.http.ResponseEntity.ok(
            java.util.Map.of("status", "NONE"));
    }
}

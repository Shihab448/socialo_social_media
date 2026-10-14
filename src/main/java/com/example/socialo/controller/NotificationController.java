package com.example.socialo.controller;

import com.example.socialo.model.User;
import com.example.socialo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class NotificationController {

    @Autowired private NotificationService notificationService;
    @Autowired private UserService userService;

    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        model.addAttribute("currentUser", user);
        model.addAttribute("notifications", notificationService.getForUser(user));
        model.addAttribute("unreadCount", notificationService.countUnread(user));
        return "notifications";
    }

    @PostMapping("/notifications/read/{id}")
    public String markRead(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        notificationService.markRead(id, user);
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllRead(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        notificationService.markAllRead(user);
        return "redirect:/notifications";
    }
}

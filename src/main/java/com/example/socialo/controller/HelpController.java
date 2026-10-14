package com.example.socialo.controller;

import com.example.socialo.model.User;
import com.example.socialo.service.NotificationService;
import com.example.socialo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelpController {

    @Autowired private UserService userService;
    @Autowired private NotificationService notificationService;

    @GetMapping("/help")
    public String help(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            User user = userService.getById(userId);
            model.addAttribute("currentUser", user);
            model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        }
        return "help";
    }
}

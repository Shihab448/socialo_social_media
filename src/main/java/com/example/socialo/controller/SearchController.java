package com.example.socialo.controller;

import com.example.socialo.model.User;
import com.example.socialo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class SearchController {

    @Autowired private UserService userService;
    @Autowired private FriendService friendService;
    @Autowired private FollowService followService;
    @Autowired private NotificationService notificationService;

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                         HttpSession session,
                         Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User currentUser = userService.getById(userId);

        List<User> results = List.of();
        if (q != null && !q.isBlank()) {
            results = userService.search(q);
            // Remove self from results
            results = results.stream()
                    .filter(u -> !u.getId().equals(userId))
                    .toList();
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("results", results);
        model.addAttribute("query", q);
        model.addAttribute("unreadNotifications", notificationService.countUnread(currentUser));

        return "search";
    }
}

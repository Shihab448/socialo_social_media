package com.example.socialo.controller;

import com.example.socialo.model.Post;
import com.example.socialo.model.User;
import com.example.socialo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Handles the Reels feed and reel upload. */
@Controller
public class ReelsController {

    @Autowired private PostService postService;
    @Autowired private UserService userService;
    @Autowired private NotificationService notificationService;

    @GetMapping("/reels")
    public String reels(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);

        List<Post> reels = postService.getReelsForUser(user);

        model.addAttribute("currentUser", user);
        model.addAttribute("reels", reels);
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        return "reels";
    }

    @PostMapping("/reels/create")
    public String createReel(HttpSession session,
                             @RequestParam(required = false) String caption,
                             @RequestParam(required = false) MultipartFile video,
                             RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        try {
            postService.createReel(user, caption, video);
            ra.addFlashAttribute("success", "Reel posted!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reels";
    }
}

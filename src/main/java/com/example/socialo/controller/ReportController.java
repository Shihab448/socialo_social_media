package com.example.socialo.controller;

import com.example.socialo.model.User;
import com.example.socialo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReportController {

    @Autowired private ReportService reportService;
    @Autowired private UserService userService;
    @Autowired private NotificationService notificationService;

    @GetMapping("/report")
    public String reportPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        model.addAttribute("currentUser", user);
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        return "report";
    }

    @GetMapping("/report/{reportedUserId}")
    public String reportUserPage(@PathVariable Long reportedUserId,
                                 HttpSession session,
                                 Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        User reportedUser = userService.getById(reportedUserId);
        model.addAttribute("currentUser", user);
        model.addAttribute("reportedUser", reportedUser);
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        return "report";
    }

    @PostMapping("/report/submit")
    public String submitReport(HttpSession session,
                               @RequestParam String category,
                               @RequestParam(required = false) String reportedUserIdentifier,
                               @RequestParam(required = false) Long reportedUserId,
                               @RequestParam(required = false) Long reportedPostId,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String priority,
                               RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        User reporter = userService.getById(userId);

        try {
            reportService.submitReport(reporter, reportedUserId, reportedPostId,
                    category, description, priority);
            redirectAttributes.addFlashAttribute("success",
                    "Your report has been submitted. Our team will review it shortly.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/home";
    }
}

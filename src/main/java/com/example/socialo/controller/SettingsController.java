package com.example.socialo.controller;

import com.example.socialo.model.User;
import com.example.socialo.model.UserSettings;
import com.example.socialo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    @Autowired private UserService userService;
    @Autowired private NotificationService notificationService;
    @Autowired private BlockService blockService;

    @GetMapping
    public String settingsPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        UserSettings settings = userService.getSettings(user);
        model.addAttribute("user", user);
        model.addAttribute("settings", settings);
        model.addAttribute("blockedUsers", blockService.getBlockedBy(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        return "settings";
    }

    // ── Save Account Info ─────────────────────────────────────

    @PostMapping("/account")
    public String saveAccount(HttpSession session,
                              @RequestParam(required = false) String firstName,
                              @RequestParam(required = false) String lastName,
                              @RequestParam(required = false) String bio,
                              RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            User updated = userService.updateProfile(userId, firstName, lastName,
                    bio, null, null, null, null, null, null, null, null);
            session.setAttribute("user", updated);
            redirectAttributes.addFlashAttribute("success", "Account settings saved.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }

    // ── Change Password ───────────────────────────────────────

    @PostMapping("/password")
    public String changePassword(HttpSession session,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam(required = false) String confirmNewPassword,
                                 RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            if (confirmNewPassword != null && !newPassword.equals(confirmNewPassword)) {
                redirectAttributes.addFlashAttribute("error", "New passwords do not match.");
                return "redirect:/settings";
            }
            userService.changePassword(userId, currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }

    // ── Privacy ───────────────────────────────────────────────

    @PostMapping("/privacy")
    public String savePrivacy(HttpSession session,
                              @RequestParam(required = false) String privateAccount,
                              @RequestParam(required = false) String showOnlineStatus,
                              @RequestParam(required = false) String allowFollowRequests,
                              RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        try {
            userService.updatePrivacy(userId, "on".equals(privateAccount));

            UserSettings settings = userService.getSettings(user);
            settings.setShowOnlineStatus("on".equals(showOnlineStatus));
            settings.setAllowFollowRequests("on".equals(allowFollowRequests));
            userService.saveSettings(user, settings);

            redirectAttributes.addFlashAttribute("success", "Privacy settings saved.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }

    // ── Delete Account ────────────────────────────────────────

    @PostMapping("/delete-account")
    public String deleteAccount(HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        // Mark as blocked / deleted (soft delete)
        userService.blockUser(userId);
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Your account has been deactivated.");
        return "redirect:/login";
    }
}

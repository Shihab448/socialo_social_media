package com.example.socialo.controller;

import com.example.socialo.model.Appeal;
import com.example.socialo.model.User;
import com.example.socialo.service.AppealService;
import com.example.socialo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Handles: signup, email verification, forgot-password, reset-password, appeal.
 * The root login/logout stays in HomeController for backward compat.
 */
@Controller
public class AuthController {

    @Autowired private UserService userService;
    @Autowired private AppealService appealService;

    // ── Sign Up ───────────────────────────────────────────────

    @GetMapping("/signup")
    public String signupPage(HttpSession session) {
        if (session.getAttribute("userId") != null) return "redirect:/home";
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String firstName,
                         @RequestParam String lastName,
                         @RequestParam(required = false) String username,
                         @RequestParam(required = false) String email,
                         @RequestParam(required = false) String phone,
                         @RequestParam String password,
                         @RequestParam(required = false) String confirmPassword,
                         @RequestParam(required = false) String dateOfBirth,
                         @RequestParam(required = false) MultipartFile profilePicture,
                         HttpSession session,
                         Model model,
                         RedirectAttributes ra) {
        try {
            if (!password.equals(confirmPassword)) {
                model.addAttribute("error", "Passwords do not match.");
                return "signup";
            }
            if (password.length() < 6) {
                model.addAttribute("error", "Password must be at least 6 characters.");
                return "signup";
            }
            if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
                model.addAttribute("error", "Please provide an email or phone number.");
                return "signup";
            }

            User user = userService.register(firstName, lastName, username, email, phone,
                                             password, dateOfBirth, profilePicture);

            // If email provided, redirect to verify page
            if (user.getEmail() != null && !user.isEmailVerified()) {
                session.setAttribute("pendingVerifyUserId", user.getId());
                ra.addFlashAttribute("info",
                    "A 6-digit verification code has been sent to " + user.getEmail() +
                    ". Check your console if SMTP is not configured.");
                return "redirect:/verify-email";
            }

            ra.addFlashAttribute("success", "Account created! Please login.");
            return "redirect:/login";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "signup";
        }
    }

    // ── Email verification ────────────────────────────────────

    @GetMapping("/verify-email")
    public String verifyPage(HttpSession session, Model model) {
        Long pendingId = (Long) session.getAttribute("pendingVerifyUserId");
        if (pendingId == null) return "redirect:/login";
        User user = userService.getById(pendingId);
        model.addAttribute("email", user.getEmail());
        return "verify-email";
    }

    @PostMapping("/verify-email")
    public String verifyEmail(@RequestParam String code,
                              HttpSession session,
                              Model model,
                              RedirectAttributes ra) {
        Long pendingId = (Long) session.getAttribute("pendingVerifyUserId");
        if (pendingId == null) return "redirect:/login";

        User user = userService.getById(pendingId);
        String err = userService.verifyEmailCode(user, code.trim());

        if (err != null) {
            model.addAttribute("error", err);
            model.addAttribute("email", user.getEmail());
            return "verify-email";
        }

        session.removeAttribute("pendingVerifyUserId");
        ra.addFlashAttribute("success", "Email verified! Please login.");
        return "redirect:/login";
    }

    @PostMapping("/verify-email/resend")
    public String resendCode(HttpSession session, RedirectAttributes ra) {
        Long pendingId = (Long) session.getAttribute("pendingVerifyUserId");
        if (pendingId == null) return "redirect:/login";
        User user = userService.getById(pendingId);
        userService.sendVerificationCode(user);
        ra.addFlashAttribute("info", "A new code has been sent.");
        return "redirect:/verify-email";
    }

    // ── Forgot password ───────────────────────────────────────

    @GetMapping("/forgot-password")
    public String forgotPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String emailOrPhone, Model model) {
        // Always show the same message (security: don't reveal if account exists)
        userService.sendPasswordResetLink(emailOrPhone);
        model.addAttribute("success",
            "If an account exists for that email, a reset link has been sent. Check your console if SMTP is not configured.");
        return "forgot-password";
    }

    // ── Reset password ────────────────────────────────────────

    @GetMapping("/reset-password")
    public String resetPage(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                Model model,
                                RedirectAttributes ra) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("token", token);
            return "reset-password";
        }
        String err = userService.resetPassword(token, newPassword);
        if (err != null) {
            model.addAttribute("error", err);
            model.addAttribute("token", token);
            return "reset-password";
        }
        ra.addFlashAttribute("success", "Password reset successfully. Please login.");
        return "redirect:/login";
    }

    // ── Appeal ────────────────────────────────────────────────

    @GetMapping("/appeal")
    public String appealPage(HttpSession session, Model model) {
        // Can be accessed even when suspended
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            Optional<User> opt = userService.findById(userId);
            opt.ifPresent(u -> model.addAttribute("user", u));
        }
        return "appeal";
    }

    @PostMapping("/appeal")
    public String submitAppeal(@RequestParam String message,
                               @RequestParam(required = false) org.springframework.web.multipart.MultipartFile documentImage,
                               HttpSession session,
                               Model model,
                               RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            model.addAttribute("error", "You must be logged in to submit an appeal.");
            return "appeal";
        }
        User user = userService.getById(userId);

        // Validation
        if (message == null || message.trim().length() < 50) {
            model.addAttribute("error", "Please write a detailed appeal message (minimum 50 characters).");
            model.addAttribute("user", user);
            return "appeal";
        }
        if (documentImage == null || documentImage.isEmpty()) {
            model.addAttribute("error", "Please upload a valid ID document (NID, Student ID, Driving Licence, or Birth Certificate).");
            model.addAttribute("user", user);
            return "appeal";
        }

        appealService.submit(user, message.trim(), documentImage);
        ra.addFlashAttribute("success",
            "Your appeal has been submitted with your document. We will review it and notify you by email.");
        return "redirect:/login";
    }
}

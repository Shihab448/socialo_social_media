package com.example.socialo.controller;

import com.example.socialo.model.User;
import com.example.socialo.service.AppealService;
import com.example.socialo.service.ReportService;
import com.example.socialo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private ReportService reportService;
    @Autowired private AppealService appealService;


    private User getAdminUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return null;
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) return null;
        User user = userService.findById(userId).orElse(null);
        if (user == null || !user.hasAdminAccess()) return null;
        return user;
    }

    private boolean requireSuperAdmin(HttpSession session, RedirectAttributes ra) {
        User u = getAdminUser(session);
        if (u == null || !u.isSuperAdmin()) {
            if (ra != null) ra.addFlashAttribute("error", "SUPER_ADMIN access required.");
            return false;
        }
        return true;
    }



    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (getAdminUser(session) != null) return "redirect:/admin/dashboard";
        return "admin/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String emailOrPhone,
                        @RequestParam String password,
                        HttpSession session, Model model) {
        try {
            User user = userService.login(emailOrPhone, password);
            if (user == null || !user.hasAdminAccess()) {
                model.addAttribute("error", "Invalid credentials or insufficient privileges.");
                return "admin/login";
            }
            session.setAttribute("userId", user.getId());
            session.setAttribute("user", user);
            session.setAttribute("isAdmin", true);
            return "redirect:/admin/dashboard";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "admin/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }



    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User admin = getAdminUser(session);
        if (admin == null) return "redirect:/admin/login";

        model.addAttribute("adminUser", admin);
        model.addAttribute("totalUsers",   userService.countTotal());
        model.addAttribute("activeUsers",  userService.countActive());
        model.addAttribute("blockedUsers", userService.countBlocked());
        model.addAttribute("totalReports", reportService.countAll());
        model.addAttribute("openReports",  reportService.countOpen());
        model.addAttribute("users",        userService.getAllUsers());
        model.addAttribute("reports",      reportService.getAll());

        if (admin.isSuperAdmin()) {
            model.addAttribute("admins",     userService.getUsersByRole(User.Role.ADMIN));
            model.addAttribute("moderators", userService.getUsersByRole(User.Role.MODERATOR));
        }
        // Appeals tab
        model.addAttribute("appeals", appealService.getAll());
        model.addAttribute("pendingAppeals", appealService.countPending());
        return "admin/dashboard";
    }



    @PostMapping("/block/{id}")
    public String block(@PathVariable Long id, HttpSession session,
                        RedirectAttributes ra) {
        if (getAdminUser(session) == null) return "redirect:/admin/login";
        try { userService.blockUser(id); ra.addFlashAttribute("success", "User blocked."); }
        catch (RuntimeException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/unblock/{id}")
    public String unblock(@PathVariable Long id, HttpSession session,
                          RedirectAttributes ra) {
        if (getAdminUser(session) == null) return "redirect:/admin/login";
        try { userService.unblockUser(id); ra.addFlashAttribute("success", "User unblocked."); }
        catch (RuntimeException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/admin/dashboard";
    }


    @PostMapping("/role/set")
    public String setRole(@RequestParam Long targetUserId,
                          @RequestParam String newRole,
                          HttpSession session, RedirectAttributes ra) {
        if (!requireSuperAdmin(session, ra)) return "redirect:/admin/dashboard";
        Long actorId = (Long) session.getAttribute("userId");
        try {
            User.Role role = User.Role.valueOf(newRole.toUpperCase());
            userService.promoteRole(actorId, targetUserId, role);
            ra.addFlashAttribute("success", "Role updated successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/role/demote/{id}")
    public String demote(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!requireSuperAdmin(session, ra)) return "redirect:/admin/dashboard";
        Long actorId = (Long) session.getAttribute("userId");
        try {
            userService.demoteToUser(actorId, id);
            ra.addFlashAttribute("success", "User demoted to USER role.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }



    @PostMapping("/report/resolve/{id}")
    public String resolve(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (getAdminUser(session) == null) return "redirect:/admin/login";
        reportService.resolve(id);
        ra.addFlashAttribute("success", "Report resolved.");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/report/dismiss/{id}")
    public String dismiss(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (getAdminUser(session) == null) return "redirect:/admin/login";
        reportService.dismiss(id);
        ra.addFlashAttribute("success", "Report dismissed.");
        return "redirect:/admin/dashboard";
    }

    // ── Appeals ───────────────────────────────────────────────

    @PostMapping("/appeal/approve/{id}")
    public String approveAppeal(@PathVariable Long id,
                                @RequestParam(required = false) String adminNote,
                                HttpSession session, RedirectAttributes ra) {
        if (getAdminUser(session) == null) return "redirect:/admin/login";
        try {
            appealService.approve(id, adminNote);
            ra.addFlashAttribute("success", "Appeal approved. Account restored.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/appeal/reject/{id}")
    public String rejectAppeal(@PathVariable Long id,
                               @RequestParam(required = false) String adminNote,
                               HttpSession session, RedirectAttributes ra) {
        if (getAdminUser(session) == null) return "redirect:/admin/login";
        try {
            appealService.reject(id, adminNote);
            ra.addFlashAttribute("success", "Appeal rejected.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}

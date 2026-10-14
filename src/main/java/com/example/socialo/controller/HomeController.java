package com.example.socialo.controller;

import com.example.socialo.model.User;
import com.example.socialo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class HomeController {

    @Autowired private UserService userService;
    @Autowired private PostService postService;
    @Autowired private StoryService storyService;
    @Autowired private NotificationService notificationService;
    @Autowired private FriendService friendService;
    @Autowired private MessageService messageService;

    @Value("${app.webrtc.stun-urls:stun:stun.l.google.com:19302}")
    private String stunUrls;

    @Value("${app.webrtc.turn-url:}")
    private String turnUrl;

    @Value("${app.webrtc.turn-username:}")
    private String turnUsername;

    @Value("${app.webrtc.turn-credential:}")
    private String turnCredential;

    // ── Root / Splash ─────────────────────────────────────────

    @GetMapping("/")
    public String splash(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return Boolean.TRUE.equals(session.getAttribute("isAdmin"))
                   ? "redirect:/admin/dashboard" : "redirect:/home";
        }
        return "splash";
    }

    // ── Login ─────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return Boolean.TRUE.equals(session.getAttribute("isAdmin"))
                   ? "redirect:/admin/dashboard" : "redirect:/home";
        }
        return "index";
    }

    @PostMapping("/login")
    public String login(@RequestParam String emailOrPhone,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        try {
            User user = userService.login(emailOrPhone, password);
            if (user == null) {
                model.addAttribute("error", "Invalid email/phone or password.");
                return "index";
            }
            session.setAttribute("userId", user.getId());
            session.setAttribute("user", user);

            if (user.hasAdminAccess()) {
                session.setAttribute("isAdmin", true);
                return "redirect:/admin/dashboard";
            }
            return "redirect:/home";

        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if ("SUSPENDED".equals(msg)) {
                // Show appeal link on login page
                model.addAttribute("suspended", true);
                model.addAttribute("error",
                    "Your account has been suspended. " +
                    "You can submit an appeal below.");
            } else {
                model.addAttribute("error", msg);
            }
            return "index";
        }
    }

    // ── Logout ────────────────────────────────────────────────

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        boolean wasAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        session.invalidate();
        return wasAdmin ? "redirect:/admin/login" : "redirect:/login";
    }

    // ── Home Feed ─────────────────────────────────────────────

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User currentUser = userService.getById(userId);
        session.setAttribute("user", currentUser);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("feed", postService.getFeedForUser(currentUser));
        model.addAttribute("stories", storyService.getFeedStories(currentUser));
        model.addAttribute("unreadNotifications", notificationService.countUnread(currentUser));
        model.addAttribute("totalChatUnread", messageService.getTotalUnread(currentUser));

        // "People You May Know" – up to 8 suggestions (non-admins only, not blocked)
        List<User> suggestions = friendService.getSuggestions(currentUser);
        // Attach relationship status so the button can show correct state
        model.addAttribute("suggestions", suggestions);
        // Pre-compute status map so template can check without calling service
        Map<Long, String> suggestionStatus = new LinkedHashMap<>();
        for (User s : suggestions) {
            suggestionStatus.put(s.getId(),
                friendService.getRelationshipStatus(currentUser, s));
        }
        model.addAttribute("suggestionStatus", suggestionStatus);

        return "home";
    }

    // ── WebRTC ICE config endpoint (authenticated) ────────────

    @GetMapping("/api/webrtc/config")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> webrtcConfig(HttpSession session) {
        if (session.getAttribute("userId") == null)
            return ResponseEntity.status(401).build();

        List<Map<String, Object>> iceServers = new ArrayList<>();

        // STUN servers (comma-separated list allowed)
        Map<String, Object> stun = new LinkedHashMap<>();
        stun.put("urls", Arrays.stream(stunUrls.split(","))
                               .map(String::trim).toList());
        iceServers.add(stun);

        // TURN server (optional)
        if (turnUrl != null && !turnUrl.isBlank()) {
            Map<String, Object> turn = new LinkedHashMap<>();
            turn.put("urls", turnUrl.trim());
            turn.put("username", turnUsername);
            turn.put("credential", turnCredential);
            iceServers.add(turn);
        }

        return ResponseEntity.ok(Map.of("iceServers", iceServers));
    }
}

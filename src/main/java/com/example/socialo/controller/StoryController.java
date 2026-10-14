package com.example.socialo.controller;

import com.example.socialo.model.Story;
import com.example.socialo.model.StoryView;
import com.example.socialo.model.User;
import com.example.socialo.repository.StoryViewRepository;
import com.example.socialo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class StoryController {

    @Autowired private StoryService storyService;
    @Autowired private UserService userService;
    @Autowired private NotificationService notificationService;
    @Autowired private StoryViewRepository storyViewRepo;

    // ── Story upload page ─────────────────────────────────────

    @GetMapping("/story")
    public String storyPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        model.addAttribute("currentUser", user);
        model.addAttribute("myStories", storyService.getActiveStoriesForUser(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        return "story-view";
    }

    // ── Create story ──────────────────────────────────────────

    @PostMapping("/story/create")
    public String createStory(HttpSession session,
                              @RequestParam(required = false) MultipartFile media,
                              @RequestParam(required = false) String caption,
                              RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        try {
            storyService.createStory(user, media, caption);
            ra.addFlashAttribute("success", "Story posted!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/home";
    }

    // ── View a specific story ─────────────────────────────────

    @GetMapping("/story/view/{id}")
    public String viewStory(@PathVariable Long id,
                            HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User currentUser = userService.getById(userId);
        Story story = storyService.getById(id);

        // Record view
        storyService.markView(story, currentUser);

        // Navigation
        List<Story> feedStories = storyService.getFeedStories(currentUser);
        int idx = 0;
        for (int i = 0; i < feedStories.size(); i++) {
            if (feedStories.get(i).getId().equals(id)) { idx = i; break; }
        }
        Long prevId = idx > 0                       ? feedStories.get(idx - 1).getId() : null;
        Long nextId = idx < feedStories.size() - 1  ? feedStories.get(idx + 1).getId() : null;

        // Viewers — ONLY the story owner can see who viewed
        List<StoryView> viewers = List.of();
        boolean isOwner = story.getUser().getId().equals(userId);
        if (isOwner) {
            viewers = storyViewRepo.findByStory(story);
        }

        model.addAttribute("story",       story);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isOwner",     isOwner);
        model.addAttribute("viewCount",   storyService.getViewCount(story));
        model.addAttribute("viewers",     viewers);
        model.addAttribute("prevStoryId", prevId);
        model.addAttribute("nextStoryId", nextId);
        model.addAttribute("feedStories", feedStories);
        model.addAttribute("unreadNotifications", notificationService.countUnread(currentUser));
        return "story-view";
    }

    // ── React to story (AJAX) ─────────────────────────────────

    @PostMapping("/story/react/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> react(
            @PathVariable Long id,
            @RequestParam(defaultValue = "❤️") String emoji,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();

        User user = userService.getById(userId);
        Story story = storyService.getById(id);

        // Notify story owner (not self)
        if (!story.getUser().getId().equals(userId)) {
            notificationService.create(
                story.getUser(), user, "STORY_REACTION", id,
                user.getFullName() + " reacted " + emoji + " to your story.");
        }
        return ResponseEntity.ok(Map.of("success", true, "emoji", emoji));
    }

    // ── Delete story ──────────────────────────────────────────

    @PostMapping("/story/delete/{id}")
    public String deleteStory(@PathVariable Long id,
                              HttpSession session, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            storyService.deleteStory(id, userId);
            ra.addFlashAttribute("success", "Story deleted.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/home";
    }
}

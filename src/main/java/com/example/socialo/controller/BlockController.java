package com.example.socialo.controller;

import com.example.socialo.model.User;
import com.example.socialo.service.BlockService;
import com.example.socialo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/** Handles block / unblock actions (profile, chat, settings). */
@Controller
public class BlockController {

    @Autowired private BlockService blockService;
    @Autowired private UserService userService;

    /** Block from profile page – redirect back. */
    @PostMapping("/block/{targetId}")
    public String block(@PathVariable Long targetId, HttpSession session,
                        RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        User blocker = userService.getById(userId);
        User blocked = userService.getById(targetId);
        blockService.block(blocker, blocked);
        ra.addFlashAttribute("success", blocked.getFirstName() + " has been blocked.");
        return "redirect:/home";
    }

    /** Unblock from Settings -> Blocked users page – redirect back. */
    @PostMapping("/unblock/{targetId}")
    public String unblock(@PathVariable Long targetId, HttpSession session,
                          RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        User blocker = userService.getById(userId);
        User blocked = userService.getById(targetId);
        blockService.unblock(blocker, blocked);
        ra.addFlashAttribute("success", blocked.getFirstName() + " has been unblocked.");
        return "redirect:/settings";
    }

    /** AJAX block – returns JSON. */
    @PostMapping("/block-ajax/{targetId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> blockAjax(@PathVariable Long targetId,
                                                         HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User blocker = userService.getById(userId);
        User blocked = userService.getById(targetId);
        blockService.block(blocker, blocked);
        return ResponseEntity.ok(Map.of("blocked", true));
    }

    /** AJAX unblock – returns JSON. */
    @PostMapping("/unblock-ajax/{targetId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> unblockAjax(@PathVariable Long targetId,
                                                           HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        User blocker = userService.getById(userId);
        User blocked = userService.getById(targetId);
        blockService.unblock(blocker, blocked);
        return ResponseEntity.ok(Map.of("blocked", false));
    }
}

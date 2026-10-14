package com.example.socialo.controller;

import com.example.socialo.model.User;
import com.example.socialo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FollowController {

    @Autowired private FollowService followService;
    @Autowired private UserService userService;

    @PostMapping("/follow/{targetId}")
    public String follow(@PathVariable Long targetId,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        User follower = userService.getById(userId);
        User following = userService.getById(targetId);
        followService.follow(follower, following);
        return "redirect:/user-profile/" + targetId;
    }

    @PostMapping("/unfollow/{targetId}")
    public String unfollow(@PathVariable Long targetId,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        User follower = userService.getById(userId);
        User following = userService.getById(targetId);
        followService.unfollow(follower, following);
        return "redirect:/user-profile/" + targetId;
    }
}

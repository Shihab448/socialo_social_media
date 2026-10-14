package com.example.socialo.controller;

import com.example.socialo.model.User;
import com.example.socialo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    @Autowired private UserService userService;
    @Autowired private PostService postService;
    @Autowired private FriendService friendService;
    @Autowired private FollowService followService;
    @Autowired private NotificationService notificationService;

    // ── Own Profile ───────────────────────────────────────────

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        session.setAttribute("user", user);

        model.addAttribute("user", user);
        model.addAttribute("posts", postService.getPostsByUser(user));
        model.addAttribute("postCount", postService.countByUser(user));
        model.addAttribute("friendCount", friendService.countFriends(user));
        model.addAttribute("followerCount", followService.countFollowers(user));
        model.addAttribute("followingCount", followService.countFollowing(user));
        model.addAttribute("friends", friendService.getFriends(user));
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        model.addAttribute("isOwnProfile", true);
        return "profile";
    }

    // ── Another User's Profile (Part 11: friend-only + profile lock) ──

    @GetMapping("/user-profile/{id}")
    public String userProfile(@PathVariable Long id,
                              HttpSession session, Model model) {
        Long currentUserId = (Long) session.getAttribute("userId");
        User currentUser = userService.getById(currentUserId);

        // Redirect own profile
        if (id.equals(currentUserId)) return "redirect:/profile";

        User targetUser = userService.getById(id);

        // ── FRIEND-ONLY ACCESS CHECK (Part 11) ────────────────
        boolean areFriends = friendService.getFriends(currentUser)
                .stream().anyMatch(f -> f.getId().equals(id));

        // Profile is locked or private → only friends can see full profile
        boolean isRestricted = (targetUser.isProfileLocked() || targetUser.isPrivate()) && !areFriends;

        if (isRestricted) {
            model.addAttribute("targetUser", targetUser);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("areFriends", false);
            model.addAttribute("isRestricted", true);
            model.addAttribute("unreadNotifications", notificationService.countUnread(currentUser));
            // Show restricted view — no posts, no private info
            return "user-profile-restricted";
        }

        // ── FULL PROFILE (friends or public) ─────────────────
        String relationshipStatus = friendService.getRelationshipStatus(currentUser, targetUser);
        boolean isFollowing = followService.isFollowing(currentUser, targetUser);

        model.addAttribute("user", targetUser);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("posts", postService.getPublicPostsByUser(targetUser));
        model.addAttribute("postCount", postService.countByUser(targetUser));
        model.addAttribute("friendCount", friendService.countFriends(targetUser));
        model.addAttribute("followerCount", followService.countFollowers(targetUser));
        model.addAttribute("followingCount", followService.countFollowing(targetUser));
        model.addAttribute("relationshipStatus", relationshipStatus);
        model.addAttribute("isFollowing", isFollowing);
        model.addAttribute("areFriends", areFriends);
        model.addAttribute("isRestricted", false);
        model.addAttribute("unreadNotifications", notificationService.countUnread(currentUser));
        return "user-profile";
    }

    // ── Edit Profile GET ──────────────────────────────────────

    @GetMapping("/edit-profile")
    public String editProfilePage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        model.addAttribute("user", user);
        return "edit-profile";
    }

    // ── Edit Profile POST ─────────────────────────────────────

    @PostMapping("/edit-profile")
    public String editProfile(HttpSession session,
                              @RequestParam(required = false) String firstName,
                              @RequestParam(required = false) String lastName,
                              @RequestParam(required = false) String bio,
                              @RequestParam(required = false) String location,
                              @RequestParam(required = false) String ssc,
                              @RequestParam(required = false) String hsc,
                              @RequestParam(required = false) String university,
                              @RequestParam(required = false) String work,
                              @RequestParam(required = false) String relationship,
                              @RequestParam(required = false) MultipartFile profileImage,
                              @RequestParam(required = false) MultipartFile coverImage,
                              @RequestParam(required = false) String croppedProfileImage,
                              @RequestParam(required = false) String croppedCoverImage,
                              RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            User updated = userService.updateProfile(userId, firstName, lastName,
                    bio, location, ssc, hsc, university, work, relationship,
                    profileImage, coverImage);

            // Save cropped images from Cropper.js (base64)
            if (croppedProfileImage != null && !croppedProfileImage.isBlank()) {
                updated = userService.updateProfileImageFromBase64(userId, croppedProfileImage, false);
            }
            if (croppedCoverImage != null && !croppedCoverImage.isBlank()) {
                updated = userService.updateProfileImageFromBase64(userId, croppedCoverImage, true);
            }

            session.setAttribute("user", updated);
            ra.addFlashAttribute("success", "Profile updated successfully.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    // ── Profile Lock toggle (Part 10) ────────────────────────

    @PostMapping("/profile/lock")
    public String lockProfile(HttpSession session, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        userService.setProfileLocked(userId, !user.isProfileLocked());
        ra.addFlashAttribute("success",
                user.isProfileLocked() ? "Profile unlocked." : "Profile locked.");
        return "redirect:/profile";
    }

    // ── Photos ────────────────────────────────────────────────

    @GetMapping("/photos")
    public String photos(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        var photos = postService.getPostsByUser(user).stream()
                .flatMap(p -> p.getMediaList().stream())
                .filter(m -> "IMAGE".equals(m.getMediaType()))
                .toList();
        model.addAttribute("user", user);
        model.addAttribute("photos", photos);
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        return "photos";
    }

    // ── Videos ────────────────────────────────────────────────

    @GetMapping("/videos")
    public String videos(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        var videos = postService.getPostsByUser(user).stream()
                .flatMap(p -> p.getMediaList().stream())
                .filter(m -> "VIDEO".equals(m.getMediaType()))
                .toList();
        model.addAttribute("user", user);
        model.addAttribute("videos", videos);
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        return "videos";
    }
}

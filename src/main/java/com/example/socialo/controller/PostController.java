package com.example.socialo.controller;

import com.example.socialo.model.*;
import com.example.socialo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class PostController {

    @Autowired private PostService postService;
    @Autowired private CommentService commentService;
    @Autowired private ReactionService reactionService;
    @Autowired private UserService userService;
    @Autowired private NotificationService notificationService;

    @GetMapping("/create-post")
    public String createPostPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        model.addAttribute("currentUser", user);
        model.addAttribute("unreadNotifications", notificationService.countUnread(user));
        return "create-post";
    }

    @PostMapping("/create-post")
    public String createPost(HttpSession session,
                             @RequestParam(required = false) String caption,
                             @RequestParam(required = false) String location,
                             @RequestParam(required = false) String visibility,
                             @RequestParam(required = false) MultipartFile[] media,
                             RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getById(userId);
        try {
            postService.createPost(user, caption, location, visibility, media);
            ra.addFlashAttribute("success", "Post created!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/home";
    }

    @GetMapping("/post/{id}")
    public String viewPost(@PathVariable Long id, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User currentUser = userService.getById(userId);
        Post post = postService.getById(id);

        List<Comment> comments = commentService.getCommentsForPost(post);
        long likeCount = reactionService.getCount(post);
        String userReactionType = reactionService.getUserReactionType(post, currentUser);
        Map<String, Long> reactionCounts = reactionService.getReactionCounts(post);

        // Check saved
        @SuppressWarnings("unchecked")
        Set<Long> savedPosts = (Set<Long>) session.getAttribute("savedPosts");
        boolean isSaved = savedPosts != null && savedPosts.contains(id);

        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("likeCount", likeCount);
        model.addAttribute("userLiked", userReactionType != null);
        model.addAttribute("userReactionType", userReactionType);
        model.addAttribute("reactionCounts", reactionCounts);
        model.addAttribute("isSaved", isSaved);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("unreadNotifications", notificationService.countUnread(currentUser));
        return "post-view";
    }

    @PostMapping("/post/edit/{id}")
    public String editPost(@PathVariable Long id, HttpSession session,
                           @RequestParam(required = false) String caption,
                           @RequestParam(required = false) String location,
                           @RequestParam(required = false) String visibility,
                           RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            postService.editPost(id, userId, caption, location, visibility);
            ra.addFlashAttribute("success", "Post updated.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/post/" + id;
    }

    @PostMapping("/post/delete/{id}")
    public String deletePost(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            postService.deletePost(id, userId);
            ra.addFlashAttribute("success", "Post deleted.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/home";
    }

    // ── React via AJAX — returns JSON ────────────────────────

    @PostMapping("/post/react/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> react(
            @PathVariable Long id,
            @RequestParam(defaultValue = "LIKE") String type,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();

        User currentUser = userService.getById(userId);
        Post post = postService.getById(id);

        boolean reacted = reactionService.setReaction(post, currentUser, type);

        if (reacted && !post.getUser().getId().equals(userId)) {
            notificationService.create(post.getUser(), currentUser, "LIKE", id,
                    currentUser.getFullName() + " reacted to your post.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reacted", reacted);
        result.put("type", reacted ? type.toUpperCase() : null);
        result.put("count", reactionService.getCount(post));
        result.put("counts", reactionService.getReactionCounts(post));
        return ResponseEntity.ok(result);
    }

    // ── Add Comment ───────────────────────────────────────────

    @PostMapping("/post/comment/{postId}")
    public String addComment(@PathVariable Long postId, HttpSession session,
                             @RequestParam String content, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        User currentUser = userService.getById(userId);
        Post post = postService.getById(postId);
        try {
            commentService.addComment(post, currentUser, content);
            if (!post.getUser().getId().equals(userId)) {
                notificationService.create(post.getUser(), currentUser, "COMMENT", postId,
                        currentUser.getFullName() + " commented on your post.");
            }
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/post/" + postId;
    }

    // ── Add Reply ─────────────────────────────────────────────

    @PostMapping("/post/comment/reply/{postId}")
    public String addReply(@PathVariable Long postId,
                           @RequestParam Long parentCommentId,
                           @RequestParam String content,
                           HttpSession session, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        User currentUser = userService.getById(userId);
        Post post = postService.getById(postId);
        try {
            commentService.addReply(post, currentUser, parentCommentId, content);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/post/" + postId;
    }

    // ── Delete Comment ────────────────────────────────────────

    @PostMapping("/post/comment/delete/{commentId}")
    public String deleteComment(@PathVariable Long commentId,
                                @RequestParam(required = false) Long postId,
                                HttpSession session, RedirectAttributes ra) {
        Long userId = (Long) session.getAttribute("userId");
        try {
            commentService.deleteComment(commentId, userId);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return postId != null ? "redirect:/post/" + postId : "redirect:/home";
    }

    // ── Save / Unsave Post (AJAX) ─────────────────────────────

    @PostMapping("/post/save/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> savePost(@PathVariable Long id,
                                                        HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();

        @SuppressWarnings("unchecked")
        Set<Long> saved = (Set<Long>) session.getAttribute("savedPosts");
        if (saved == null) saved = new LinkedHashSet<>();

        boolean isSaved;
        if (saved.contains(id)) { saved.remove(id); isSaved = false; }
        else                    { saved.add(id);    isSaved = true;  }
        session.setAttribute("savedPosts", saved);

        Map<String, Object> res = new HashMap<>();
        res.put("saved", isSaved);
        return ResponseEntity.ok(res);
    }

    // ── Saved Posts Page ──────────────────────────────────────

    @GetMapping("/saved")
    public String savedPosts(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        User currentUser = userService.getById(userId);

        @SuppressWarnings("unchecked")
        Set<Long> savedIds = (Set<Long>) session.getAttribute("savedPosts");
        List<Post> savedPosts = new ArrayList<>();
        if (savedIds != null) {
            for (Long pid : savedIds) {
                try { savedPosts.add(postService.getById(pid)); }
                catch (Exception ignored) {}
            }
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("posts", savedPosts);
        model.addAttribute("unreadNotifications", notificationService.countUnread(currentUser));
        return "home";
    }
}

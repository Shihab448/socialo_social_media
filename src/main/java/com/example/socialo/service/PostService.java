package com.example.socialo.service;

import com.example.socialo.model.*;
import com.example.socialo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    @Autowired private PostRepository postRepository;
    @Autowired private PostMediaRepository postMediaRepository;
    @Autowired private FileStorageService fileStorageService;

    // ── Create ────────────────────────────────────────────────

    @Transactional
    public Post createPost(User user, String caption, String location,
                           String visibility, MultipartFile[] media) {
        Post post = new Post();
        post.setUser(user);
        post.setCaption(caption);
        post.setLocation(location);
        post.setVisibility(visibility != null ? visibility : "PUBLIC");
        Post saved = postRepository.save(post);

        if (media != null) {
            for (MultipartFile file : media) {
                if (file != null && !file.isEmpty()) {
                    String url = fileStorageService.saveFile(file);
                    PostMedia pm = new PostMedia();
                    pm.setPost(saved);
                    pm.setMediaUrl(url);
                    pm.setMediaType(fileStorageService.isVideo(file) ? "VIDEO" : "IMAGE");
                    postMediaRepository.save(pm);
                }
            }
        }
        return saved;
    }

    // ── Feed ──────────────────────────────────────────────────

    public List<Post> getFeedForUser(User user) {
        return postRepository.findFeedForUser(user);
    }

    // ── By User ───────────────────────────────────────────────

    public List<Post> getPostsByUser(User user) {
        return postRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Post> getPublicPostsByUser(User user) {
        return postRepository.findPublicAndFriendPostsByUser(user);
    }

    // ── Get Single ────────────────────────────────────────────

    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }

    public Post getById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found: " + id));
    }

    // ── Edit ──────────────────────────────────────────────────

    @Transactional
    public Post editPost(Long postId, Long currentUserId, String caption,
                         String location, String visibility) {
        Post post = getById(postId);
        if (!post.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("You can only edit your own posts.");
        }
        post.setCaption(caption);
        post.setLocation(location);
        if (visibility != null) post.setVisibility(visibility);
        return postRepository.save(post);
    }

    // ── Delete ────────────────────────────────────────────────

    @Transactional
    public void deletePost(Long postId, Long currentUserId) {
        Post post = getById(postId);
        if (!post.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("You can only delete your own posts.");
        }
        // Delete associated media files
        for (PostMedia pm : post.getMediaList()) {
            fileStorageService.deleteFile(pm.getMediaUrl());
        }
        postRepository.delete(post);
    }

    public long countByUser(User user) {
        return postRepository.countByUser(user);
    }

    public List<Post> getReelsForUser(User user) {
        return postRepository.findReelsForUser(user);
    }

    @Transactional
    public Post createReel(User user, String caption, MultipartFile video) {
        if (video == null || video.isEmpty())
            throw new RuntimeException("Please select a video for the reel.");
        if (!fileStorageService.isVideo(video))
            throw new RuntimeException("Reels must be a video file.");

        Post post = new Post();
        post.setUser(user);
        post.setCaption(caption);
        post.setVisibility("PUBLIC");
        post.setReel(true);
        Post saved = postRepository.save(post);

        String url = fileStorageService.saveFile(video);
        PostMedia pm = new PostMedia();
        pm.setPost(saved);
        pm.setMediaUrl(url);
        pm.setMediaType("VIDEO");
        postMediaRepository.save(pm);

        return saved;
    }
}

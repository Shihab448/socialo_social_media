package com.example.socialo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 5000)
    private String caption;

    private String location;

    @Column(nullable = false)
    private String visibility = "PUBLIC";

    /** True for short video reels (shown in the Reels feed). */
    @Column(nullable = false, columnDefinition = "boolean")
    private boolean isReel = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PostMedia> mediaList = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Reaction> reactions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public boolean isReel() { return isReel; }
    public void setReel(boolean reel) { isReel = reel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<PostMedia> getMediaList() { return mediaList; }
    public void setMediaList(List<PostMedia> mediaList) { this.mediaList = mediaList; }
    public List<Comment> getComments() { return comments; }
    public List<Reaction> getReactions() { return reactions; }
    public int getLikeCount() { return reactions.size(); }
    public int getCommentCount() { return comments.size(); }

    public String getFirstImageUrl() {
        return mediaList.stream()
                .filter(m -> "IMAGE".equals(m.getMediaType()))
                .findFirst().map(PostMedia::getMediaUrl).orElse(null);
    }

    public String getFirstVideoUrl() {
        return mediaList.stream()
                .filter(m -> "VIDEO".equals(m.getMediaType()))
                .findFirst().map(PostMedia::getMediaUrl).orElse(null);
    }
}

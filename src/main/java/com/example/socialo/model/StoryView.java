package com.example.socialo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "story_views",
       uniqueConstraints = @UniqueConstraint(columnNames = {"story_id", "viewer_id"}))
public class StoryView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "viewer_id", nullable = false)
    private User viewer;

    @Column(nullable = false, updatable = false)
    private LocalDateTime viewedAt;

    @PrePersist
    protected void onCreate() {
        this.viewedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Story getStory() { return story; }
    public void setStory(Story story) { this.story = story; }
    public User getViewer() { return viewer; }
    public void setViewer(User viewer) { this.viewer = viewer; }
    public LocalDateTime getViewedAt() { return viewedAt; }
}

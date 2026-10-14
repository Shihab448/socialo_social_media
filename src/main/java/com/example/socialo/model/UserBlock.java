package com.example.socialo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Records that 'blocker' has blocked 'blocked'. Unique pair. */
@Entity
@Table(name = "user_blocks",
       uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"}))
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    // ── getters / setters ─────────────────────────────────────

    public Long getId() { return id; }

    public User getBlocker() { return blocker; }
    public void setBlocker(User v) { this.blocker = v; }

    public User getBlocked() { return blocked; }
    public void setBlocked(User v) { this.blocked = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

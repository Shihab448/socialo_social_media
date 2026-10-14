package com.example.socialo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Emoji reaction on a chat message. One reaction per user per message. */
@Entity
@Table(name = "message_reactions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id"}))
public class MessageReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Single emoji character or short string, e.g. "👍", "❤️", "😂". */
    @Column(nullable = false, length = 10)
    private String emoji;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    // ── getters / setters ─────────────────────────────────────

    public Long getId() { return id; }

    public Message getMessage() { return message; }
    public void setMessage(Message v) { this.message = v; }

    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String v) { this.emoji = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

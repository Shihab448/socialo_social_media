package com.example.socialo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Membership record linking a user to a conversation with their role and read pointer. */
@Entity
@Table(name = "conversation_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"}))
public class ConversationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** ADMIN or MEMBER */
    @Column(nullable = false, columnDefinition = "varchar(10)")
    private String role = "MEMBER";

    /**
     * ID of the last message the user has read.
     * Used to compute unread count and show "Seen" ticks.
     */
    @Column(columnDefinition = "bigint")
    private Long lastReadMessageId = 0L;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() { this.joinedAt = LocalDateTime.now(); }

    // ── getters / setters ─────────────────────────────────────

    public Long getId() { return id; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation v) { this.conversation = v; }

    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }

    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }

    public Long getLastReadMessageId() { return lastReadMessageId; }
    public void setLastReadMessageId(Long v) { this.lastReadMessageId = v; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
}

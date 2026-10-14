package com.example.socialo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A suspension appeal submitted by a blocked user.
 * Admin can APPROVE (restores account) or REJECT.
 */
@Entity
@Table(name = "appeals")
public class Appeal {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 3000, nullable = false)
    private String message;

    /** Path to uploaded ID/document image for identity verification. */
    private String documentImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(10)")
    private Status status = Status.PENDING;

    /** Admin note added when approving or rejecting. */
    @Column(length = 1000)
    private String adminNote;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    // ── getters / setters ─────────────────────────────────────

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }

    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }

    public String getDocumentImage() { return documentImage; }
    public void setDocumentImage(String v) { this.documentImage = v; }

    public Status getStatus() { return status; }
    public void setStatus(Status v) { this.status = v; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String v) { this.adminNote = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime v) { this.resolvedAt = v; }
}

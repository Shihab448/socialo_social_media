package com.example.socialo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores email-verification codes (6-digit) and password-reset tokens (UUID link).
 * A token is valid for 15 minutes (verification code) or 30 minutes (reset link).
 * After 5 wrong code attempts the token is locked.
 */
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {

    public enum Type { EMAIL_VERIFY, PASSWORD_RESET }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The 6-digit code or UUID token string. */
    @Column(nullable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, columnDefinition = "boolean")
    private boolean used = false;

    /** Number of wrong code attempts (for EMAIL_VERIFY). Max 5. */
    @Column(nullable = false, columnDefinition = "int")
    private int attempts = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }
    public boolean isLocked()  { return attempts >= 5; }

    // ── getters / setters ─────────────────────────────────────

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }

    public String getToken() { return token; }
    public void setToken(String v) { this.token = v; }

    public Type getType() { return type; }
    public void setType(Type v) { this.type = v; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime v) { this.expiresAt = v; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean v) { this.used = v; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int v) { this.attempts = v; }
    public void incrementAttempts() { this.attempts++; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

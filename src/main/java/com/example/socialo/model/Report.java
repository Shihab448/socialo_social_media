package com.example.socialo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    // ID of the reported user (nullable if reporting a post)
    private Long reportedUserId;

    // ID of the reported post (nullable if reporting a user)
    private Long reportedPostId;

    // FAKE_ACCOUNT, HARASSMENT, SPAM, INAPPROPRIATE_CONTENT, ABUSIVE_MESSAGE, OTHER
    @Column(nullable = false)
    private String category;

    @Column(length = 3000)
    private String description;

    // OPEN, RESOLVED, DISMISSED
    @Column(nullable = false)
    private String status = "OPEN";

    private String priority = "NORMAL";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──────────────────────────────────────

    public Long getId() { return id; }

    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }

    public Long getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(Long reportedUserId) { this.reportedUserId = reportedUserId; }

    public Long getReportedPostId() { return reportedPostId; }
    public void setReportedPostId(Long reportedPostId) { this.reportedPostId = reportedPostId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

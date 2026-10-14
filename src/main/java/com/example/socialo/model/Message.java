package com.example.socialo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A single message in a conversation. Supports text, images, video, files, audio, calls, system messages. */
@Entity
@Table(name = "messages")
public class Message {

    /** TEXT, IMAGE, VIDEO, FILE, AUDIO, CALL, SYSTEM */
    public enum MediaType { TEXT, IMAGE, VIDEO, FILE, AUDIO, CALL, SYSTEM }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(length = 5000)
    private String content;

    /** URL in /uploads/ for media/file/audio messages. */
    private String mediaUrl;

    /** TEXT by default. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(10)")
    private MediaType mediaType = MediaType.TEXT;

    /** Original filename shown in the file card. */
    private String fileName;

    /** File size in bytes, shown next to the filename. */
    @Column(columnDefinition = "bigint")
    private Long fileSize = 0L;

    /** The message this is a reply to (nullable). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id")
    private Message replyTo;

    /** Soft-delete: true = "You unsent a message". */
    @Column(nullable = false, columnDefinition = "boolean")
    private boolean isDeleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    // ── helpers ───────────────────────────────────────────────

    public String getDisplayContent() {
        if (isDeleted) return "You unsent a message";
        return content;
    }

    public boolean isMedia() {
        return mediaType != MediaType.TEXT && mediaType != MediaType.SYSTEM;
    }

    // ── getters / setters ─────────────────────────────────────

    public Long getId() { return id; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation v) { this.conversation = v; }

    public User getSender() { return sender; }
    public void setSender(User v) { this.sender = v; }

    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String v) { this.mediaUrl = v; }

    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType v) { this.mediaType = v; }

    public String getFileName() { return fileName; }
    public void setFileName(String v) { this.fileName = v; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long v) { this.fileSize = v; }

    public Message getReplyTo() { return replyTo; }
    public void setReplyTo(Message v) { this.replyTo = v; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean v) { this.isDeleted = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

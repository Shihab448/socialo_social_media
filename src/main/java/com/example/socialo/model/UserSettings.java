package com.example.socialo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private boolean showOnlineStatus = true;
    private boolean allowFollowRequests = true;
    private boolean likeNotifications = true;
    private boolean commentNotifications = true;
    private boolean friendRequestNotifications = true;
    private boolean messageNotifications = true;
    private String theme = "light";
    private String fontSize = "medium";

    // ── Getters & Setters ──────────────────────────────────────

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public boolean isShowOnlineStatus() { return showOnlineStatus; }
    public void setShowOnlineStatus(boolean v) { this.showOnlineStatus = v; }

    public boolean isAllowFollowRequests() { return allowFollowRequests; }
    public void setAllowFollowRequests(boolean v) { this.allowFollowRequests = v; }

    public boolean isLikeNotifications() { return likeNotifications; }
    public void setLikeNotifications(boolean v) { this.likeNotifications = v; }

    public boolean isCommentNotifications() { return commentNotifications; }
    public void setCommentNotifications(boolean v) { this.commentNotifications = v; }

    public boolean isFriendRequestNotifications() { return friendRequestNotifications; }
    public void setFriendRequestNotifications(boolean v) { this.friendRequestNotifications = v; }

    public boolean isMessageNotifications() { return messageNotifications; }
    public void setMessageNotifications(boolean v) { this.messageNotifications = v; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getFontSize() { return fontSize; }
    public void setFontSize(String fontSize) { this.fontSize = fontSize; }
}

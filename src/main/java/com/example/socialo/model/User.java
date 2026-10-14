package com.example.socialo.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Socialo user account. Roles: USER, MODERATOR, ADMIN, SUPER_ADMIN. */
@Entity
@Table(name = "users")
public class User {

    public enum Role { USER, MODERATOR, ADMIN, SUPER_ADMIN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    @Column(nullable = false)
    private String password;

    private LocalDate dateOfBirth;

    @Column(length = 1000)
    private String bio;

    private String location;
    private String ssc;
    private String hsc;
    private String university;
    private String work;
    private String relationship;
    private String profileImage;
    private String coverImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private Role role = Role.USER;

    /** kept for DB backward-compat – synced with role */
    @Column(nullable = false, columnDefinition = "boolean")
    private boolean isAdmin = false;

    @Column(nullable = false, columnDefinition = "boolean")
    private boolean isPrivate = false;

    @Column(nullable = false, columnDefinition = "boolean")
    private boolean profileLocked = false;

    @Column(nullable = false, columnDefinition = "boolean")
    private boolean isBlocked = false;

    /** True once the user has verified their e-mail address. */
    @Column(nullable = false, columnDefinition = "boolean")
    private boolean emailVerified = false;

    /** Updated on WebSocket connect/disconnect – used for online status. */
    @Column(columnDefinition = "timestamp")
    private LocalDateTime lastActiveAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
        this.isAdmin = (this.role == Role.ADMIN || this.role == Role.SUPER_ADMIN);
    }

    // ── helpers ───────────────────────────────────────────────

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public String getDisplayProfileImage() {
        return (profileImage != null && !profileImage.isBlank()) ? profileImage : "/images/default-avatar.png";
    }

    public String getDisplayCoverImage() {
        return (coverImage != null && !coverImage.isBlank()) ? coverImage : "/images/default-cover.jpg";
    }

    public boolean hasAdminAccess() {
        return role == Role.MODERATOR || role == Role.ADMIN || role == Role.SUPER_ADMIN;
    }

    public boolean isSuperAdmin() { return role == Role.SUPER_ADMIN; }
    public boolean isModerator()  { return role == Role.MODERATOR; }

    /** Returns how long ago the user was active, e.g. "Active now", "Active 5m ago". */
    public String getActiveStatus() {
        if (lastActiveAt == null) return "Offline";
        long mins = java.time.Duration.between(lastActiveAt, LocalDateTime.now()).toMinutes();
        if (mins < 2)  return "Active now";
        if (mins < 60) return "Active " + mins + "m ago";
        long hrs = mins / 60;
        if (hrs < 24)  return "Active " + hrs + "h ago";
        return "Active " + (hrs / 24) + "d ago";
    }

    // ── getters / setters ─────────────────────────────────────

    public Long getId() { return id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String v) { this.firstName = v; }

    public String getLastName() { return lastName; }
    public void setLastName(String v) { this.lastName = v; }

    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }

    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }

    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }

    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate v) { this.dateOfBirth = v; }

    public String getBio() { return bio; }
    public void setBio(String v) { this.bio = v; }

    public String getLocation() { return location; }
    public void setLocation(String v) { this.location = v; }

    public String getSsc() { return ssc; }
    public void setSsc(String v) { this.ssc = v; }

    public String getHsc() { return hsc; }
    public void setHsc(String v) { this.hsc = v; }

    public String getUniversity() { return university; }
    public void setUniversity(String v) { this.university = v; }

    public String getWork() { return work; }
    public void setWork(String v) { this.work = v; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String v) { this.relationship = v; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String v) { this.profileImage = v; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String v) { this.coverImage = v; }

    public Role getRole() { return role; }
    public void setRole(Role v) {
        this.role = v;
        this.isAdmin = (v == Role.ADMIN || v == Role.SUPER_ADMIN);
    }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean v) { this.isAdmin = v; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean v) { this.isPrivate = v; }

    public boolean isProfileLocked() { return profileLocked; }
    public void setProfileLocked(boolean v) { this.profileLocked = v; }

    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean v) { this.isBlocked = v; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean v) { this.emailVerified = v; }

    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime v) { this.lastActiveAt = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

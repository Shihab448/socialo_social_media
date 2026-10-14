package com.example.socialo.service;

import com.example.socialo.model.User;
import com.example.socialo.model.UserSettings;
import com.example.socialo.model.VerificationToken;
import com.example.socialo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Core user account service: register, login, email verification, password reset, profile updates. */
@Service
public class UserService {

    @Autowired private UserRepository userRepo;
    @Autowired private UserSettingsRepository settingsRepo;
    @Autowired private VerificationTokenRepository tokenRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private FileStorageService fileStorage;
    @Autowired private EmailService emailService;

    private final SecureRandom rng = new SecureRandom();

    // ── Registration ──────────────────────────────────────────

    @Transactional
    public User register(String firstName, String lastName, String username,
                         String email, String phone, String password,
                         String dateOfBirthStr, MultipartFile profilePicture) {

        if (email != null && !email.isBlank() && userRepo.existsByEmail(email.trim()))
            throw new RuntimeException("An account with this email already exists.");
        if (phone != null && !phone.isBlank() && userRepo.existsByPhone(phone.trim()))
            throw new RuntimeException("An account with this phone number already exists.");

        // Email is now required
        if (email == null || email.isBlank())
            throw new RuntimeException("An email address is required to create an account.");

        String cleanUsername = (username != null && !username.trim().isBlank()) ? username.trim() : null;
        if (cleanUsername != null && userRepo.existsByUsername(cleanUsername))
            throw new RuntimeException("This username is already taken.");

        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setUsername(cleanUsername);
        user.setEmail(email != null && !email.isBlank() ? email.trim() : null);
        user.setPhone(phone != null && !phone.isBlank() ? phone.trim() : null);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(User.Role.USER);
        // Email verification required if email provided
        user.setEmailVerified(user.getEmail() == null);

        if (dateOfBirthStr != null && !dateOfBirthStr.isBlank()) {
            try { user.setDateOfBirth(java.time.LocalDate.parse(dateOfBirthStr)); }
            catch (Exception ignored) {}
        }
        if (profilePicture != null && !profilePicture.isEmpty())
            user.setProfileImage(fileStorage.saveFile(profilePicture));

        User saved = userRepo.save(user);

        UserSettings s = new UserSettings();
        s.setUser(saved);
        settingsRepo.save(s);

        // Send verification code if email provided
        if (saved.getEmail() != null) {
            String code = sendVerificationCode(saved);
            // code printed to console if SMTP not configured
        }

        return saved;
    }

    /** Generate and send a new 6-digit verification code. Returns the code. */
    @Transactional
    public String sendVerificationCode(User user) {
        // Invalidate old tokens
        tokenRepo.findTopByUserAndTypeOrderByCreatedAtDesc(user, VerificationToken.Type.EMAIL_VERIFY)
                 .ifPresent(t -> { t.setUsed(true); tokenRepo.save(t); });

        String code = String.format("%06d", rng.nextInt(1_000_000));
        VerificationToken vt = new VerificationToken();
        vt.setUser(user);
        vt.setToken(code);
        vt.setType(VerificationToken.Type.EMAIL_VERIFY);
        vt.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        tokenRepo.save(vt);

        if (user.getEmail() != null)
            emailService.sendVerificationCode(user.getEmail(), user.getFullName(), code);
        return code;
    }

    /** Verify the 6-digit code. Returns error message or null on success. */
    @Transactional
    public String verifyEmailCode(User user, String code) {
        Optional<VerificationToken> opt = tokenRepo
                .findTopByUserAndTypeOrderByCreatedAtDesc(user, VerificationToken.Type.EMAIL_VERIFY);
        if (opt.isEmpty()) return "No verification code found. Please request a new one.";

        VerificationToken vt = opt.get();
        if (vt.isUsed())    return "This code has already been used.";
        if (vt.isExpired()) return "This code has expired. Please request a new one.";
        if (vt.isLocked())  return "Too many wrong attempts. Please request a new code.";

        if (!vt.getToken().equals(code)) {
            vt.incrementAttempts();
            tokenRepo.save(vt);
            int left = 5 - vt.getAttempts();
            return left > 0 ? "Wrong code. " + left + " attempt(s) remaining."
                            : "Too many wrong attempts. Please request a new code.";
        }

        vt.setUsed(true);
        tokenRepo.save(vt);
        user.setEmailVerified(true);
        userRepo.save(user);
        return null; // success
    }

    // ── Login ─────────────────────────────────────────────────

    public User login(String emailOrPhone, String password) {
        Optional<User> opt = userRepo.findByEmailOrPhone(emailOrPhone);
        if (opt.isEmpty()) return null;

        User user = opt.get();
        if (user.isBlocked())
            throw new RuntimeException("SUSPENDED");  // caller checks for this key
        if (!passwordEncoder.matches(password, user.getPassword())) return null;
        return user;
    }

    // ── Forgot / Reset password ───────────────────────────────

    @Transactional
    public void sendPasswordResetLink(String emailOrPhone) {
        Optional<User> opt = userRepo.findByEmailOrPhone(emailOrPhone);
        if (opt.isEmpty()) return; // same message regardless

        User user = opt.get();
        if (user.getEmail() == null) return;

        // Invalidate old reset tokens
        tokenRepo.findTopByUserAndTypeOrderByCreatedAtDesc(user, VerificationToken.Type.PASSWORD_RESET)
                 .ifPresent(t -> { t.setUsed(true); tokenRepo.save(t); });

        String token = UUID.randomUUID().toString().replace("-", "");
        VerificationToken vt = new VerificationToken();
        vt.setUser(user);
        vt.setToken(token);
        vt.setType(VerificationToken.Type.PASSWORD_RESET);
        vt.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        tokenRepo.save(vt);

        emailService.sendPasswordReset(user.getEmail(), user.getFullName(), token);
    }

    /** Returns error message or null on success. */
    @Transactional
    public String resetPassword(String token, String newPassword) {
        Optional<VerificationToken> opt = tokenRepo.findByTokenAndType(token, VerificationToken.Type.PASSWORD_RESET);
        if (opt.isEmpty()) return "Invalid or expired reset link.";

        VerificationToken vt = opt.get();
        if (vt.isUsed())    return "This link has already been used.";
        if (vt.isExpired()) return "This reset link has expired. Please request a new one.";
        if (newPassword.length() < 6) return "Password must be at least 6 characters.";

        vt.setUsed(true);
        tokenRepo.save(vt);

        User user = vt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        return null;
    }

    // ── Find ──────────────────────────────────────────────────

    public Optional<User> findById(Long id) { return userRepo.findById(id); }

    public User getById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    // ── Profile update ────────────────────────────────────────

    @Transactional
    public User updateProfile(Long userId, String firstName, String lastName,
                              String bio, String location, String ssc, String hsc,
                              String university, String work, String relationship,
                              MultipartFile profilePicture, MultipartFile coverPhoto) {
        User user = getById(userId);
        if (firstName != null && !firstName.isBlank()) user.setFirstName(firstName.trim());
        if (lastName  != null && !lastName.isBlank())  user.setLastName(lastName.trim());
        user.setBio(bio);
        user.setLocation(location);
        user.setSsc(ssc);
        user.setHsc(hsc);
        user.setUniversity(university);
        user.setWork(work);
        user.setRelationship(relationship);
        if (profilePicture != null && !profilePicture.isEmpty()) {
            fileStorage.deleteFile(user.getProfileImage());
            user.setProfileImage(fileStorage.saveFile(profilePicture));
        }
        if (coverPhoto != null && !coverPhoto.isEmpty()) {
            fileStorage.deleteFile(user.getCoverImage());
            user.setCoverImage(fileStorage.saveFile(coverPhoto));
        }
        return userRepo.save(user);
    }

    @Transactional
    public User updateProfileImageFromBase64(Long userId, String base64, boolean isCover) {
        User user = getById(userId);
        if (base64 == null || base64.isBlank()) return user;
        String url = fileStorage.saveBase64Image(base64);
        if (isCover) { fileStorage.deleteFile(user.getCoverImage()); user.setCoverImage(url); }
        else         { fileStorage.deleteFile(user.getProfileImage()); user.setProfileImage(url); }
        return userRepo.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword()))
            throw new RuntimeException("Current password is incorrect.");
        if (newPassword.length() < 6)
            throw new RuntimeException("New password must be at least 6 characters.");
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    // ── Online presence ───────────────────────────────────────

    @Transactional
    public void touchLastActive(Long userId) {
        userRepo.findById(userId).ifPresent(u -> {
            u.setLastActiveAt(LocalDateTime.now());
            userRepo.save(u);
        });
    }

    // ── Block/unblock + admin ─────────────────────────────────

    @Transactional
    public void blockUser(Long userId) {
        User user = getById(userId);
        if (user.isSuperAdmin()) throw new RuntimeException("Cannot suspend a Super Admin.");
        user.setBlocked(true);
        userRepo.save(user);
        if (user.getEmail() != null)
            emailService.sendSuspensionNotice(user.getEmail(), user.getFullName());
    }

    @Transactional
    public void unblockUser(Long userId) {
        User user = getById(userId);
        user.setBlocked(false);
        userRepo.save(user);
        if (user.getEmail() != null)
            emailService.sendRestorationNotice(user.getEmail(), user.getFullName());
    }

    @Transactional
    public void promoteRole(Long actorId, Long targetId, User.Role newRole) {
        User actor = getById(actorId);
        if (!actor.isSuperAdmin()) throw new RuntimeException("Only SUPER_ADMIN can change roles.");
        User target = getById(targetId);
        if (target.isSuperAdmin()) throw new RuntimeException("Cannot change role of another SUPER_ADMIN.");
        if (newRole == User.Role.SUPER_ADMIN) throw new RuntimeException("Cannot create new SUPER_ADMIN.");
        target.setRole(newRole);
        userRepo.save(target);
    }

    @Transactional
    public void demoteToUser(Long actorId, Long targetId) { promoteRole(actorId, targetId, User.Role.USER); }

    @Transactional
    public void setProfileLocked(Long userId, boolean locked) {
        User user = getById(userId);
        user.setProfileLocked(locked);
        user.setPrivate(locked);
        userRepo.save(user);
    }

    @Transactional
    public void updatePrivacy(Long userId, boolean isPrivate) {
        User user = getById(userId);
        user.setPrivate(isPrivate);
        userRepo.save(user);
    }

    // ── Search + stats ────────────────────────────────────────

    public List<User> search(String query) {
        if (query == null || query.isBlank()) return List.of();
        return userRepo.searchUsers(query.trim());
    }

    public long countTotal()   { return userRepo.count(); }
    public long countActive()  { return userRepo.countActiveUsers(); }
    public long countBlocked() { return userRepo.countBlockedUsers(); }
    public List<User> getAllUsers() { return userRepo.findAll(); }
    public List<User> getUsersByRole(User.Role role) { return userRepo.findByRole(role); }

    // ── Settings ──────────────────────────────────────────────

    public UserSettings getSettings(User user) {
        return settingsRepo.findByUser(user).orElseGet(() -> {
            UserSettings s = new UserSettings();
            s.setUser(user);
            return settingsRepo.save(s);
        });
    }

    @Transactional
    public void saveSettings(User user, UserSettings settings) {
        UserSettings ex = getSettings(user);
        ex.setShowOnlineStatus(settings.isShowOnlineStatus());
        ex.setAllowFollowRequests(settings.isAllowFollowRequests());
        ex.setLikeNotifications(settings.isLikeNotifications());
        ex.setCommentNotifications(settings.isCommentNotifications());
        ex.setFriendRequestNotifications(settings.isFriendRequestNotifications());
        ex.setMessageNotifications(settings.isMessageNotifications());
        ex.setTheme(settings.getTheme());
        ex.setFontSize(settings.getFontSize());
        settingsRepo.save(ex);
    }
}

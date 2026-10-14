package com.example.socialo.service;

import com.example.socialo.model.Appeal;
import com.example.socialo.model.User;
import com.example.socialo.repository.AppealRepository;
import com.example.socialo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Handles suspension appeals from users. */
@Service
public class AppealService {

    @Autowired private AppealRepository appealRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private EmailService emailService;
    @Autowired private FileStorageService fileStorage;

    @Transactional
    public Appeal submit(User user, String message, org.springframework.web.multipart.MultipartFile documentImage) {
        Appeal appeal = new Appeal();
        appeal.setUser(user);
        appeal.setMessage(message);
        appeal.setStatus(Appeal.Status.PENDING);
        if (documentImage != null && !documentImage.isEmpty()) {
            String url = fileStorage.saveFile(documentImage);
            appeal.setDocumentImage(url);
        }
        return appealRepo.save(appeal);
    }

    public List<Appeal> getAll() {
        return appealRepo.findAllByOrderByCreatedAtDesc();
    }

    public List<Appeal> getPending() {
        return appealRepo.findByStatus(Appeal.Status.PENDING);
    }

    public long countPending() {
        return appealRepo.countByStatus(Appeal.Status.PENDING);
    }

    @Transactional
    public void approve(Long appealId, String adminNote) {
        Appeal appeal = appealRepo.findById(appealId)
                .orElseThrow(() -> new RuntimeException("Appeal not found"));
        appeal.setStatus(Appeal.Status.APPROVED);
        appeal.setAdminNote(adminNote);
        appeal.setResolvedAt(LocalDateTime.now());
        appealRepo.save(appeal);

        // Restore account
        User user = appeal.getUser();
        user.setBlocked(false);
        userRepo.save(user);

        if (user.getEmail() != null)
            emailService.sendRestorationNotice(user.getEmail(), user.getFullName());
    }

    @Transactional
    public void reject(Long appealId, String adminNote) {
        Appeal appeal = appealRepo.findById(appealId)
                .orElseThrow(() -> new RuntimeException("Appeal not found"));
        appeal.setStatus(Appeal.Status.REJECTED);
        appeal.setAdminNote(adminNote);
        appeal.setResolvedAt(LocalDateTime.now());
        appealRepo.save(appeal);

        User user = appeal.getUser();
        if (user.getEmail() != null)
            emailService.sendAppealRejected(user.getEmail(), user.getFullName(), adminNote);
    }
}

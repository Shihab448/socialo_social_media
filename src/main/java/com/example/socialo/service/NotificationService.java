package com.example.socialo.service;

import com.example.socialo.model.Notification;
import com.example.socialo.model.User;
import com.example.socialo.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    @Autowired private NotificationRepository notificationRepository;

    @Transactional
    public void create(User recipient, User actor, String type, Long referenceId, String message) {
        if (recipient.getId().equals(actor != null ? actor.getId() : -1L)) return; // no self-notify
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setActor(actor);
        n.setType(type);
        n.setReferenceId(referenceId);
        n.setMessage(message);
        notificationRepository.save(n);
    }

    public List<Notification> getForUser(User user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
    }

    public long countUnread(User user) {
        return notificationRepository.countUnread(user);
    }

    @Transactional
    public void markRead(Long notificationId, User currentUser) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getRecipient().getId().equals(currentUser.getId())) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllRead(User user) {
        notificationRepository.markAllReadForUser(user);
    }
}

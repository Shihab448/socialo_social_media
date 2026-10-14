package com.example.socialo.service;

import com.example.socialo.model.*;
import com.example.socialo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FriendService {

    @Autowired private FriendRequestRepository friendRequestRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService;

    // ── Send Request ──────────────────────────────────────────

    @Transactional
    public void sendRequest(User sender, User receiver) {
        if (sender.getId().equals(receiver.getId())) {
            throw new RuntimeException("You cannot send a friend request to yourself.");
        }
        if (friendshipRepository.areFriends(sender, receiver)) {
            throw new RuntimeException("You are already friends.");
        }
        Optional<FriendRequest> existing = friendRequestRepository.findBetweenUsers(sender, receiver);
        if (existing.isPresent() && "PENDING".equals(existing.get().getStatus())) {
            throw new RuntimeException("A friend request is already pending.");
        }

        FriendRequest req = new FriendRequest();
        req.setSender(sender);
        req.setReceiver(receiver);
        req.setStatus("PENDING");
        friendRequestRepository.save(req);

        notificationService.create(receiver, sender, "FRIEND_REQUEST", null,
                sender.getFullName() + " sent you a friend request.");
    }

    // ── Accept ────────────────────────────────────────────────

    @Transactional
    public void acceptRequest(Long requestId, User currentUser) {
        FriendRequest req = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found."));
        if (!req.getReceiver().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Not authorized.");
        }
        req.setStatus("ACCEPTED");
        friendRequestRepository.save(req);

        Friendship friendship = new Friendship();
        friendship.setUser1(req.getSender());
        friendship.setUser2(req.getReceiver());
        friendshipRepository.save(friendship);

        notificationService.create(req.getSender(), currentUser, "FRIEND_ACCEPTED", null,
                currentUser.getFullName() + " accepted your friend request.");
    }

    // ── Reject ────────────────────────────────────────────────

    @Transactional
    public void rejectRequest(Long requestId, User currentUser) {
        FriendRequest req = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found."));
        if (!req.getReceiver().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Not authorized.");
        }
        req.setStatus("REJECTED");
        friendRequestRepository.save(req);
    }

    // ── Cancel ────────────────────────────────────────────────

    @Transactional
    public void cancelRequest(User sender, User receiver) {
        Optional<FriendRequest> req = friendRequestRepository.findBySenderAndReceiver(sender, receiver);
        req.ifPresent(friendRequestRepository::delete);
    }

    // ── Remove Friend ─────────────────────────────────────────

    @Transactional
    public void removeFriend(User user, User friend) {
        Optional<Friendship> fs = friendshipRepository.findBetweenUsers(user, friend);
        fs.ifPresent(friendshipRepository::delete);
    }

    // ── Get Friends ───────────────────────────────────────────

    public List<User> getFriends(User user) {
        return friendshipRepository.findByUser(user).stream()
                .map(f -> f.getOtherUser(user.getId()))
                .collect(Collectors.toList());
    }

    // ── Get Pending Requests ──────────────────────────────────

    public List<FriendRequest> getPendingRequests(User user) {
        return friendRequestRepository.findPendingRequestsForUser(user);
    }

    // ── Get Sent Requests ─────────────────────────────────────

    public List<FriendRequest> getSentRequests(User user) {
        return friendRequestRepository.findSentPendingRequests(user);
    }

    // ── Status ────────────────────────────────────────────────

    public String getRelationshipStatus(User currentUser, User targetUser) {
        if (friendshipRepository.areFriends(currentUser, targetUser)) return "FRIENDS";
        Optional<FriendRequest> req = friendRequestRepository.findBetweenUsers(currentUser, targetUser);
        if (req.isPresent() && "PENDING".equals(req.get().getStatus())) {
            if (req.get().getSender().getId().equals(currentUser.getId())) return "REQUEST_SENT";
            else return "REQUEST_RECEIVED";
        }
        return "NONE";
    }

    // ── Suggestions ───────────────────────────────────────────

    public List<User> getSuggestions(User user) {
        List<User> friends = getFriends(user);
        List<Long> friendIds = friends.stream().map(User::getId).collect(Collectors.toList());
        friendIds.add(user.getId());

        return userRepository.findAll().stream()
                .filter(u -> !friendIds.contains(u.getId()))
                .filter(u -> u.getRole() == User.Role.USER) // exclude admins/moderators
                .limit(10)
                .collect(Collectors.toList());
    }

    public long countFriends(User user) {
        return friendshipRepository.countFriends(user);
    }
}

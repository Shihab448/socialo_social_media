package com.example.socialo.service;

import com.example.socialo.model.*;
import com.example.socialo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/** Handles all chat / conversation logic. */
@Service
public class MessageService {

    @Autowired private ConversationRepository convRepo;
    @Autowired private ConversationMemberRepository memberRepo;
    @Autowired private MessageRepository msgRepo;
    @Autowired private MessageReactionRepository reactionRepo;
    @Autowired private FileStorageService fileStorage;

    // ── Conversations ─────────────────────────────────────────

    @Transactional
    public Conversation getOrCreateDm(User u1, User u2) {
        return convRepo.findDmBetween(u1, u2).orElseGet(() -> {
            Conversation c = new Conversation();
            c.setGroup(false);
            c = convRepo.save(c);
            addMember(c, u1, "MEMBER");
            addMember(c, u2, "MEMBER");
            return c;
        });
    }

    @Transactional
    public Conversation createGroup(String name, User creator, List<User> members) {
        Conversation c = new Conversation();
        c.setGroup(true);
        c.setGroupName(name);
        c = convRepo.save(c);
        addMember(c, creator, "ADMIN");
        for (User m : members) if (!m.getId().equals(creator.getId())) addMember(c, m, "MEMBER");
        postSystemMessage(c, creator.getFullName() + " created the group.");
        return c;
    }

    private void addMember(Conversation c, User u, String role) {
        if (!memberRepo.existsByConversationAndUser(c, u)) {
            ConversationMember cm = new ConversationMember();
            cm.setConversation(c); cm.setUser(u); cm.setRole(role);
            memberRepo.save(cm);
        }
    }

    // ── Group management ──────────────────────────────────────

    @Transactional
    public void addGroupMember(Conversation conv, User actor, User newMember) {
        requireGroupAdmin(conv, actor);
        addMember(conv, newMember, "MEMBER");
        postSystemMessage(conv, actor.getFullName() + " added " + newMember.getFullName() + ".");
    }

    @Transactional
    public void removeGroupMember(Conversation conv, User actor, User target) {
        requireGroupAdmin(conv, actor);
        memberRepo.findByConversationAndUser(conv, target).ifPresent(memberRepo::delete);
        postSystemMessage(conv, actor.getFullName() + " removed " + target.getFullName() + ".");
    }

    @Transactional
    public void leaveGroup(Conversation conv, User user) {
        memberRepo.findByConversationAndUser(conv, user).ifPresent(memberRepo::delete);
        postSystemMessage(conv, user.getFullName() + " left the group.");
    }

    @Transactional
    public void renameGroup(Conversation conv, User actor, String newName) {
        requireGroupAdmin(conv, actor);
        conv.setGroupName(newName);
        convRepo.save(conv);
        postSystemMessage(conv, actor.getFullName() + " renamed the group to \"" + newName + "\".");
    }

    @Transactional
    public void updateGroupPhoto(Conversation conv, User actor, MultipartFile photo) {
        requireGroupAdmin(conv, actor);
        fileStorage.deleteFile(conv.getGroupImage());
        conv.setGroupImage(fileStorage.saveFile(photo));
        convRepo.save(conv);
    }

    @Transactional
    public void deleteGroup(Conversation conv, User actor) {
        // Only the original creator (first ADMIN) can delete
        requireGroupAdmin(conv, actor);
        convRepo.delete(conv);
    }

    private void requireGroupAdmin(Conversation conv, User actor) {
        ConversationMember cm = memberRepo.findByConversationAndUser(conv, actor)
                .orElseThrow(() -> new RuntimeException("Not a member."));
        if (!"ADMIN".equals(cm.getRole()))
            throw new RuntimeException("Only group admins can do this.");
    }

    // ── Send messages ─────────────────────────────────────────

    @Transactional
    public Message sendTextMessage(Conversation conv, User sender, String content, Long replyToId) {
        requireMember(conv, sender);
        Message msg = new Message();
        msg.setConversation(conv);
        msg.setSender(sender);
        msg.setContent(content);
        msg.setMediaType(Message.MediaType.TEXT);
        if (replyToId != null)
            msgRepo.findById(replyToId).ifPresent(msg::setReplyTo);
        return msgRepo.save(msg);
    }

    @Transactional
    public Message sendMediaMessage(Conversation conv, User sender, MultipartFile file, String caption) {
        requireMember(conv, sender);
        String url = fileStorage.saveFile(file);
        Message.MediaType mt = detectMediaType(file);

        Message msg = new Message();
        msg.setConversation(conv);
        msg.setSender(sender);
        msg.setMediaUrl(url);
        msg.setMediaType(mt);
        msg.setFileName(file.getOriginalFilename());
        msg.setFileSize(file.getSize());
        if (caption != null && !caption.isBlank()) msg.setContent(caption);
        return msgRepo.save(msg);
    }

    /** Soft-delete a message (unsend). */
    @Transactional
    public void unsend(Long messageId, Long actorId) {
        msgRepo.findById(messageId).ifPresent(m -> {
            if (!m.getSender().getId().equals(actorId))
                throw new RuntimeException("You can only unsend your own messages.");
            m.setDeleted(true);
            msgRepo.save(m);
        });
    }

    private Message.MediaType detectMediaType(MultipartFile f) {
        String ct = f.getContentType() != null ? f.getContentType() : "";
        if (ct.startsWith("image/"))  return Message.MediaType.IMAGE;
        if (ct.startsWith("video/"))  return Message.MediaType.VIDEO;
        if (ct.startsWith("audio/"))  return Message.MediaType.AUDIO;
        return Message.MediaType.FILE;
    }

    // ── Message reactions ─────────────────────────────────────

    @Transactional
    public void toggleReaction(Long messageId, User user, String emoji) {
        Message msg = msgRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        Optional<MessageReaction> existing = reactionRepo.findByMessageAndUser(msg, user);
        if (existing.isPresent()) {
            if (existing.get().getEmoji().equals(emoji)) {
                reactionRepo.delete(existing.get());
            } else {
                existing.get().setEmoji(emoji);
                reactionRepo.save(existing.get());
            }
        } else {
            MessageReaction r = new MessageReaction();
            r.setMessage(msg); r.setUser(user); r.setEmoji(emoji);
            reactionRepo.save(r);
        }
    }

    public List<MessageReaction> getReactions(Long messageId) {
        return msgRepo.findById(messageId)
                .map(reactionRepo::findByMessage)
                .orElse(List.of());
    }

    // ── Read receipts ─────────────────────────────────────────

    @Transactional
    public void markRead(Conversation conv, User user, Long lastMessageId) {
        memberRepo.findByConversationAndUser(conv, user).ifPresent(cm -> {
            if (lastMessageId > cm.getLastReadMessageId()) {
                cm.setLastReadMessageId(lastMessageId);
                memberRepo.save(cm);
            }
        });
    }

    public long getUnreadCount(Conversation conv, User user) {
        ConversationMember cm = memberRepo.findByConversationAndUser(conv, user).orElse(null);
        if (cm == null) return 0;
        return msgRepo.countUnread(conv, cm.getLastReadMessageId(), user.getId());
    }

    /** Sum of unread across all conversations for a user. */
    public long getTotalUnread(User user) {
        return convRepo.findConversationsForUser(user).stream()
                .mapToLong(c -> getUnreadCount(c, user))
                .sum();
    }

    // ── Queries ───────────────────────────────────────────────

    public List<Conversation> getConversationsForUser(User user) {
        return convRepo.findConversationsForUser(user);
    }

    /** Latest 30 messages. */
    public List<Message> getMessages(Conversation conv) {
        return msgRepo.findByConversationOrderByCreatedAtAsc(conv);
    }

    /** Older messages before a given id (pagination). */
    public List<Message> getOlderMessages(Conversation conv, Long beforeId) {
        List<Message> reversed = msgRepo.findOlderMessages(conv, beforeId, PageRequest.of(0, 30));
        Collections.reverse(reversed);
        return reversed;
    }

    public Optional<Message> getLatestMessage(Conversation conv) {
        return msgRepo.findTopByConversationOrderByCreatedAtDesc(conv);
    }

    public Optional<Conversation> findById(Long id) { return convRepo.findById(id); }

    public Optional<Message> findMessageById(Long id) { return msgRepo.findById(id); }

    public boolean isMember(Conversation conv, User user) {
        return memberRepo.existsByConversationAndUser(conv, user);
    }

    public User getOtherUser(Conversation conv, User currentUser) {
        return memberRepo.findByConversation(conv).stream()
                .map(ConversationMember::getUser)
                .filter(u -> !u.getId().equals(currentUser.getId()))
                .findFirst().orElse(null);
    }

    // ── Helper ────────────────────────────────────────────────

    private void requireMember(Conversation conv, User user) {
        if (!memberRepo.existsByConversationAndUser(conv, user))
            throw new RuntimeException("You are not a member of this conversation.");
    }

    private void postSystemMessage(Conversation conv, String text) {
        Message msg = new Message();
        msg.setConversation(conv);
        msg.setSender(conv.getMembers().isEmpty() ? null :
                      conv.getMembers().get(0).getUser());
        msg.setContent(text);
        msg.setMediaType(Message.MediaType.SYSTEM);
        msgRepo.save(msg);
    }
}

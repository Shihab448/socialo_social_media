package com.example.socialo.repository;

import com.example.socialo.model.Conversation;
import com.example.socialo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // All conversations for a user (ordered by latest message)
    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN c.members m WHERE m.user = :user " +
           "ORDER BY c.createdAt DESC")
    List<Conversation> findConversationsForUser(@Param("user") User user);

    // Find direct (non-group) conversation between two users
    @Query("SELECT c FROM Conversation c WHERE c.isGroup = false AND " +
           "EXISTS (SELECT m1 FROM ConversationMember m1 WHERE m1.conversation = c AND m1.user = :u1) AND " +
           "EXISTS (SELECT m2 FROM ConversationMember m2 WHERE m2.conversation = c AND m2.user = :u2)")
    Optional<Conversation> findDmBetween(@Param("u1") User u1, @Param("u2") User u2);
}

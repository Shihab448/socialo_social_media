package com.example.socialo.repository;

import com.example.socialo.model.Conversation;
import com.example.socialo.model.ConversationMember;
import com.example.socialo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    List<ConversationMember> findByConversation(Conversation conversation);

    Optional<ConversationMember> findByConversationAndUser(Conversation conversation, User user);

    boolean existsByConversationAndUser(Conversation conversation, User user);
}

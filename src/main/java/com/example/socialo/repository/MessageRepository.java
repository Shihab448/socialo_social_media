package com.example.socialo.repository;

import com.example.socialo.model.Conversation;
import com.example.socialo.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Latest 30 messages in a conversation (for initial load). */
    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);

    /** Older messages before a given message id (for scroll-up pagination). */
    @Query("SELECT m FROM Message m WHERE m.conversation = :conv AND m.id < :beforeId ORDER BY m.id DESC")
    List<Message> findOlderMessages(@Param("conv") Conversation conversation,
                                    @Param("beforeId") Long beforeId,
                                    Pageable pageable);

    Optional<Message> findTopByConversationOrderByCreatedAtDesc(Conversation conversation);

    /** Count messages after lastReadMessageId to compute unread badge. */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation = :conv AND m.id > :lastRead AND m.sender.id != :userId")
    long countUnread(@Param("conv") Conversation conversation,
                     @Param("lastRead") Long lastReadMessageId,
                     @Param("userId") Long userId);
}

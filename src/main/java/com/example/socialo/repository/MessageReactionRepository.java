package com.example.socialo.repository;

import com.example.socialo.model.Message;
import com.example.socialo.model.MessageReaction;
import com.example.socialo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    List<MessageReaction> findByMessage(Message message);

    Optional<MessageReaction> findByMessageAndUser(Message message, User user);

    void deleteByMessage(Message message);
}

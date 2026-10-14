package com.example.socialo.repository;

import com.example.socialo.model.User;
import com.example.socialo.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByTokenAndType(String token, VerificationToken.Type type);

    /** Latest verification token of a given type for a user. */
    Optional<VerificationToken> findTopByUserAndTypeOrderByCreatedAtDesc(User user, VerificationToken.Type type);

    void deleteByUser(User user);
}

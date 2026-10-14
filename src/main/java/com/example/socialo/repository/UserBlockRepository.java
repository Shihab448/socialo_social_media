package com.example.socialo.repository;

import com.example.socialo.model.User;
import com.example.socialo.model.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    Optional<UserBlock> findByBlockerAndBlocked(User blocker, User blocked);

    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    /** All users that 'blocker' has blocked. */
    List<UserBlock> findByBlocker(User blocker);

    /** All block records where this user is involved (as blocker OR blocked). */
    List<UserBlock> findByBlockerOrBlocked(User blocker, User blocked);
}

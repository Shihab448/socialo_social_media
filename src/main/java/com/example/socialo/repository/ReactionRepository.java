package com.example.socialo.repository;

import com.example.socialo.model.Post;
import com.example.socialo.model.Reaction;
import com.example.socialo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByPostAndUser(Post post, User user);

    long countByPost(Post post);

    boolean existsByPostAndUser(Post post, User user);

    List<Reaction> findByPost(Post post);

    // Count per reaction type for a post
    @Query("SELECT r.reactionType, COUNT(r) FROM Reaction r WHERE r.post = :post GROUP BY r.reactionType")
    List<Object[]> countByTypeForPost(@Param("post") Post post);
}

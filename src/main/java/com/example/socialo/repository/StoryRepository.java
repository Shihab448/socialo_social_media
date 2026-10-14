package com.example.socialo.repository;

import com.example.socialo.model.Story;
import com.example.socialo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StoryRepository extends JpaRepository<Story, Long> {

    @Query("SELECT s FROM Story s WHERE s.user = :user AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    // Stories from friends + following that haven't expired
    @Query("SELECT s FROM Story s WHERE s.expiresAt > :now AND (" +
           "s.user = :user OR " +
           "s.user.id IN (SELECT f.user2.id FROM Friendship f WHERE f.user1 = :user) OR " +
           "s.user.id IN (SELECT f.user1.id FROM Friendship f WHERE f.user2 = :user) OR " +
           "s.user.id IN (SELECT fo.following.id FROM Follow fo WHERE fo.follower = :user)) " +
           "ORDER BY s.createdAt DESC")
    List<Story> findFeedStoriesForUser(@Param("user") User user, @Param("now") LocalDateTime now);

    void deleteByExpiresAtBefore(LocalDateTime now);
}

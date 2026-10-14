package com.example.socialo.repository;

import com.example.socialo.model.Post;
import com.example.socialo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByUserOrderByCreatedAtDesc(User user);

    // Feed: posts from friends + following + own posts
    @Query("SELECT DISTINCT p FROM Post p WHERE p.user = :user OR " +
           "p.user.id IN (SELECT f.user2.id FROM Friendship f WHERE f.user1 = :user) OR " +
           "p.user.id IN (SELECT f.user1.id FROM Friendship f WHERE f.user2 = :user) OR " +
           "p.user.id IN (SELECT fo.following.id FROM Follow fo WHERE fo.follower = :user) " +
           "ORDER BY p.createdAt DESC")
    List<Post> findFeedForUser(@Param("user") User user);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.user = :user")
    long countByUser(@Param("user") User user);

    @Query("SELECT p FROM Post p WHERE p.isReel = true AND (" +
           "p.user = :user OR " +
           "p.user.id IN (SELECT f.user2.id FROM Friendship f WHERE f.user1 = :user) OR " +
           "p.user.id IN (SELECT f.user1.id FROM Friendship f WHERE f.user2 = :user) OR " +
           "p.user.id IN (SELECT fo.following.id FROM Follow fo WHERE fo.follower = :user)) " +
           "ORDER BY p.createdAt DESC")
    List<Post> findReelsForUser(@Param("user") User user);

    @Query("SELECT p FROM Post p WHERE p.user = :user AND p.visibility IN ('PUBLIC', 'FRIENDS') ORDER BY p.createdAt DESC")
    List<Post> findPublicAndFriendPostsByUser(@Param("user") User user);
}

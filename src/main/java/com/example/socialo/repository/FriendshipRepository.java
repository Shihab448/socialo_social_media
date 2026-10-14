package com.example.socialo.repository;

import com.example.socialo.model.Friendship;
import com.example.socialo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE f.user1 = :user OR f.user2 = :user")
    List<Friendship> findByUser(@Param("user") User user);

    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.user1 = :u1 AND f.user2 = :u2) OR (f.user1 = :u2 AND f.user2 = :u1)")
    Optional<Friendship> findBetweenUsers(@Param("u1") User u1, @Param("u2") User u2);

    @Query("SELECT COUNT(f) FROM Friendship f WHERE f.user1 = :user OR f.user2 = :user")
    long countFriends(@Param("user") User user);

    default boolean areFriends(User u1, User u2) {
        return findBetweenUsers(u1, u2).isPresent();
    }
}

package com.example.socialo.repository;

import com.example.socialo.model.FriendRequest;
import com.example.socialo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    Optional<FriendRequest> findBySenderAndReceiver(User sender, User receiver);

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.receiver = :user AND fr.status = 'PENDING'")
    List<FriendRequest> findPendingRequestsForUser(@Param("user") User user);

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.sender = :user AND fr.status = 'PENDING'")
    List<FriendRequest> findSentPendingRequests(@Param("user") User user);

    @Query("SELECT fr FROM FriendRequest fr WHERE " +
           "(fr.sender = :u1 AND fr.receiver = :u2) OR (fr.sender = :u2 AND fr.receiver = :u1)")
    Optional<FriendRequest> findBetweenUsers(@Param("u1") User u1, @Param("u2") User u2);
}

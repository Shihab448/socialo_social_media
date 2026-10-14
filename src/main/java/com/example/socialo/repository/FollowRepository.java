package com.example.socialo.repository;

import com.example.socialo.model.Follow;
import com.example.socialo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerAndFollowing(User follower, User following);

    boolean existsByFollowerAndFollowing(User follower, User following);

    List<Follow> findByFollower(User follower);

    List<Follow> findByFollowing(User following);

    @Query("SELECT COUNT(f) FROM Follow f WHERE f.following = :user")
    long countFollowers(@Param("user") User user);

    @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower = :user")
    long countFollowing(@Param("user") User user);
}

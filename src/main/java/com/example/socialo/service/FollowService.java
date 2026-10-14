package com.example.socialo.service;

import com.example.socialo.model.Follow;
import com.example.socialo.model.User;
import com.example.socialo.repository.FollowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FollowService {

    @Autowired private FollowRepository followRepository;
    @Autowired private NotificationService notificationService;

    @Transactional
    public void follow(User follower, User following) {
        if (follower.getId().equals(following.getId())) return;
        if (!followRepository.existsByFollowerAndFollowing(follower, following)) {
            Follow follow = new Follow();
            follow.setFollower(follower);
            follow.setFollowing(following);
            followRepository.save(follow);

            notificationService.create(following, follower, "FOLLOW", null,
                    follower.getFullName() + " started following you.");
        }
    }

    @Transactional
    public void unfollow(User follower, User following) {
        Optional<Follow> f = followRepository.findByFollowerAndFollowing(follower, following);
        f.ifPresent(followRepository::delete);
    }

    public boolean isFollowing(User follower, User following) {
        return followRepository.existsByFollowerAndFollowing(follower, following);
    }

    public List<User> getFollowers(User user) {
        return followRepository.findByFollowing(user).stream()
                .map(Follow::getFollower)
                .collect(Collectors.toList());
    }

    public List<User> getFollowing(User user) {
        return followRepository.findByFollower(user).stream()
                .map(Follow::getFollowing)
                .collect(Collectors.toList());
    }

    public long countFollowers(User user) {
        return followRepository.countFollowers(user);
    }

    public long countFollowing(User user) {
        return followRepository.countFollowing(user);
    }
}

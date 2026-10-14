package com.example.socialo.service;

import com.example.socialo.model.User;
import com.example.socialo.model.UserBlock;
import com.example.socialo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the user-block relationship.
 * When A blocks B: friendship, follow (both directions) and pending
 * friend requests are deleted automatically.
 */
@Service
public class BlockService {

    @Autowired private UserBlockRepository blockRepo;
    @Autowired private FriendshipRepository friendshipRepo;
    @Autowired private FriendRequestRepository friendRequestRepo;
    @Autowired private FollowRepository followRepo;

    @Transactional
    public void block(User blocker, User blocked) {
        if (blocker.getId().equals(blocked.getId())) return;
        if (blockRepo.existsByBlockerAndBlocked(blocker, blocked)) return;

        // Remove friendship
        friendshipRepo.findBetweenUsers(blocker, blocked)
                      .ifPresent(friendshipRepo::delete);

        // Remove follow in both directions
        followRepo.findByFollowerAndFollowing(blocker, blocked).ifPresent(followRepo::delete);
        followRepo.findByFollowerAndFollowing(blocked, blocker).ifPresent(followRepo::delete);

        // Remove any pending friend requests
        friendRequestRepo.findBetweenUsers(blocker, blocked)
                         .ifPresent(friendRequestRepo::delete);

        UserBlock ub = new UserBlock();
        ub.setBlocker(blocker);
        ub.setBlocked(blocked);
        blockRepo.save(ub);
    }

    @Transactional
    public void unblock(User blocker, User blocked) {
        blockRepo.findByBlockerAndBlocked(blocker, blocked)
                 .ifPresent(blockRepo::delete);
    }

    /** True if either user has blocked the other. */
    public boolean isBlockedEither(User a, User b) {
        return blockRepo.existsByBlockerAndBlocked(a, b)
            || blockRepo.existsByBlockerAndBlocked(b, a);
    }

    /** True if 'blocker' specifically blocked 'blocked'. */
    public boolean isBlocked(User blocker, User blocked) {
        return blockRepo.existsByBlockerAndBlocked(blocker, blocked);
    }

    /** Return list of users that the given user has blocked. */
    public List<User> getBlockedBy(User blocker) {
        return blockRepo.findByBlocker(blocker).stream()
                        .map(UserBlock::getBlocked)
                        .collect(Collectors.toList());
    }
}

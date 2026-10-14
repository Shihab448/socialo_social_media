package com.example.socialo.service;

import com.example.socialo.model.Post;
import com.example.socialo.model.Reaction;
import com.example.socialo.model.User;
import com.example.socialo.repository.ReactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ReactionService {

    @Autowired private ReactionRepository reactionRepository;

    private static final Set<String> VALID_TYPES =
            Set.of("LIKE", "LOVE", "HAHA", "WOW", "SAD", "ANGRY");

    /**
     * Set or change reaction.
     * - If user has no reaction → create with given type.
     * - If user already has same type → remove (toggle off).
     * - If user has different type → update to new type.
     * Returns true if a reaction now exists, false if removed.
     */
    @Transactional
    public boolean setReaction(Post post, User user, String reactionType) {
        String type = (reactionType != null && VALID_TYPES.contains(reactionType.toUpperCase()))
                ? reactionType.toUpperCase() : "LIKE";

        Optional<Reaction> existing = reactionRepository.findByPostAndUser(post, user);

        if (existing.isPresent()) {
            Reaction r = existing.get();
            if (r.getReactionType().equals(type)) {
                // Same type → toggle off (remove)
                reactionRepository.delete(r);
                return false;
            } else {
                // Different type → update
                r.setReactionType(type);
                reactionRepository.save(r);
                return true;
            }
        } else {
            // No reaction → create
            Reaction r = new Reaction();
            r.setPost(post);
            r.setUser(user);
            r.setReactionType(type);
            reactionRepository.save(r);
            return true;
        }
    }

    /** Legacy toggle (LIKE only) — kept for backward compat */
    @Transactional
    public boolean toggleReaction(Post post, User user) {
        return setReaction(post, user, "LIKE");
    }

    public long getCount(Post post) {
        return reactionRepository.countByPost(post);
    }

    public boolean hasUserReacted(Post post, User user) {
        return reactionRepository.existsByPostAndUser(post, user);
    }

    /** Returns the current user's reaction type, or null */
    public String getUserReactionType(Post post, User user) {
        return reactionRepository.findByPostAndUser(post, user)
                .map(Reaction::getReactionType)
                .orElse(null);
    }

    /** Returns map of reactionType → count for a post */
    public Map<String, Long> getReactionCounts(Post post) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : reactionRepository.countByTypeForPost(post)) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return counts;
    }
}

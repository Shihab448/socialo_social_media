package com.example.socialo.repository;

import com.example.socialo.model.Comment;
import com.example.socialo.model.Post;
import com.example.socialo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Top-level comments only (not replies)
    List<Comment> findByPostAndParentCommentIsNullOrderByCreatedAtAsc(Post post);

    // Replies to a specific comment
    List<Comment> findByParentCommentOrderByCreatedAtAsc(Comment parentComment);

    // All comments for a post (including replies) — used for counts
    List<Comment> findByPost(Post post);

    long countByPost(Post post);

    List<Comment> findByUser(User user);
}

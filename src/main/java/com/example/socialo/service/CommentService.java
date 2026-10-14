package com.example.socialo.service;

import com.example.socialo.model.Comment;
import com.example.socialo.model.Post;
import com.example.socialo.model.User;
import com.example.socialo.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    @Autowired private CommentRepository commentRepository;

    @Transactional
    public Comment addComment(Post post, User user, String content) {
        if (content == null || content.isBlank()) {
            throw new RuntimeException("Comment cannot be empty.");
        }
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content.trim());
        return commentRepository.save(comment);
    }

    /** Add a reply to a parent comment */
    @Transactional
    public Comment addReply(Post post, User user, Long parentCommentId, String content) {
        if (content == null || content.isBlank()) {
            throw new RuntimeException("Reply cannot be empty.");
        }
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new RuntimeException("Parent comment not found."));
        // Don't allow reply-to-reply (keep max 1 level deep)
        if (parent.getParentComment() != null) {
            parent = parent.getParentComment();
        }
        Comment reply = new Comment();
        reply.setPost(post);
        reply.setUser(user);
        reply.setContent(content.trim());
        reply.setParentComment(parent);
        return commentRepository.save(reply);
    }

    /** Top-level comments only (not replies) */
    public List<Comment> getCommentsForPost(Post post) {
        return commentRepository.findByPostAndParentCommentIsNullOrderByCreatedAtAsc(post);
    }

    @Transactional
    public void deleteComment(Long commentId, Long currentUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found."));
        if (!comment.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("You can only delete your own comments.");
        }
        commentRepository.delete(comment);
    }

    public long countForPost(Post post) {
        return commentRepository.countByPost(post);
    }
}

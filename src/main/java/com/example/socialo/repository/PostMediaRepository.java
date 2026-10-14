package com.example.socialo.repository;

import com.example.socialo.model.Post;
import com.example.socialo.model.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {

    List<PostMedia> findByPost(Post post);

    List<PostMedia> findByPostAndMediaType(Post post, String mediaType);
}

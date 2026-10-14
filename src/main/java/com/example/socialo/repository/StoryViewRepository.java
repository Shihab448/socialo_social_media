package com.example.socialo.repository;

import com.example.socialo.model.Story;
import com.example.socialo.model.StoryView;
import com.example.socialo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryViewRepository extends JpaRepository<StoryView, Long> {

    boolean existsByStoryAndViewer(Story story, User viewer);

    long countByStory(Story story);

    List<StoryView> findByStory(Story story);
}

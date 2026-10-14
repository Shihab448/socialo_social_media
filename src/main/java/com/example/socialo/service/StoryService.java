package com.example.socialo.service;

import com.example.socialo.model.Story;
import com.example.socialo.model.StoryView;
import com.example.socialo.model.User;
import com.example.socialo.repository.StoryRepository;
import com.example.socialo.repository.StoryViewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StoryService {

    @Autowired private StoryRepository storyRepository;
    @Autowired private StoryViewRepository storyViewRepository;
    @Autowired private FileStorageService fileStorageService;

    @Transactional
    public Story createStory(User user, MultipartFile media, String caption) {
        Story story = new Story();
        story.setUser(user);
        story.setCaption(caption);

        if (media != null && !media.isEmpty()) {
            String url = fileStorageService.saveFile(media);
            story.setMediaUrl(url);
            story.setMediaType(fileStorageService.isVideo(media) ? "VIDEO" : "IMAGE");
        } else {
            story.setMediaType("TEXT");
        }

        return storyRepository.save(story);
    }

    public List<Story> getActiveStoriesForUser(User user) {
        return storyRepository.findActiveStoriesByUser(user, LocalDateTime.now());
    }

    public List<Story> getFeedStories(User user) {
        return storyRepository.findFeedStoriesForUser(user, LocalDateTime.now());
    }

    public Story getById(Long id) {
        return storyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Story not found."));
    }

    @Transactional
    public void deleteStory(Long storyId, Long currentUserId) {
        Story story = getById(storyId);
        if (!story.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("You can only delete your own stories.");
        }
        fileStorageService.deleteFile(story.getMediaUrl());
        storyRepository.delete(story);
    }

    @Transactional
    public void markView(Story story, User viewer) {
        if (!storyViewRepository.existsByStoryAndViewer(story, viewer)) {
            StoryView view = new StoryView();
            view.setStory(story);
            view.setViewer(viewer);
            storyViewRepository.save(view);
        }
    }

    public long getViewCount(Story story) {
        return storyViewRepository.countByStory(story);
    }
}

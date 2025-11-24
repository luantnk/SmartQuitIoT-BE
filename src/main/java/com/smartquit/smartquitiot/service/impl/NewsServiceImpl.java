package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.CreateNewsRequest;
import com.smartquit.smartquitiot.dto.response.NewsDTO;
import com.smartquit.smartquitiot.entity.News;
import com.smartquit.smartquitiot.entity.NewsMedia;
import com.smartquit.smartquitiot.enums.MediaType;
import com.smartquit.smartquitiot.enums.NewsStatus;
import com.smartquit.smartquitiot.mapper.NewsMapper;
import com.smartquit.smartquitiot.repository.NewsMediaRepository;
import com.smartquit.smartquitiot.repository.NewsRepository;
import com.smartquit.smartquitiot.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final NewsMediaRepository newsMediaRepository;

    private static final Set<String> VIDEO_EXTENSIONS = Set.of(".mp4", ".avi", ".mov", ".mkv", ".webm", ".flv");

    @Override
    public List<NewsDTO> getLatestNews(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        Page<News> latestPage = newsRepository.findByStatusOrderByCreatedAtDesc(
                NewsStatus.PUBLISH,
                PageRequest.of(0, limit)
        );

        return latestPage.getContent().stream()
                .map(newsMapper::toNewsDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<NewsDTO> getAllNews(String query) {
        List<News> news;
        if (query == null || query.isBlank()) {
            Page<News> newsPage = newsRepository.findByStatusOrderByCreatedAtDesc(
                    NewsStatus.PUBLISH,
                    PageRequest.of(0, Integer.MAX_VALUE)
            );
            news = newsPage.getContent();
        } else {
            news = newsRepository.searchNews(query.toLowerCase().trim());
        }
        return news.stream()
                .map(newsMapper::toNewsDTO)
                .collect(Collectors.toList());
    }

    @Override
    public NewsDTO getNewsDetail(int id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("News not found with id: " + id));

//        if (news.getStatus() == NewsStatus.DELETED) {
//            throw new IllegalArgumentException("News has been deleted");
//        }

        return newsMapper.toNewsDTO(news);
    }

    @Override
    @Transactional
    public NewsDTO createNews(CreateNewsRequest createNewsRequest) {
        validateNewsRequest(createNewsRequest);

        News news = new News();
        news.setTitle(createNewsRequest.getTitle().trim());
        news.setContent(createNewsRequest.getContent().trim());
        news.setStatus(createNewsRequest.getNewsStatus() != null
                ? createNewsRequest.getNewsStatus()
                : NewsStatus.DRAFT);

        if (createNewsRequest.getThumbnailUrl() != null && !createNewsRequest.getThumbnailUrl().isBlank()) {
            news.setThumbnailUrl(createNewsRequest.getThumbnailUrl().trim());
        }

        // Create news media
        if (createNewsRequest.getMediaUrls() != null && !createNewsRequest.getMediaUrls().isEmpty()) {
            List<NewsMedia> newsMedia = createNewsMediaList(createNewsRequest.getMediaUrls(), news);
            news.setNewsMedia(newsMedia);
        }

        News savedNews = newsRepository.save(news);
        return newsMapper.toNewsDTO(savedNews);
    }

    @Override
    @Transactional
    public NewsDTO updateNews(int id, CreateNewsRequest updateRequest) {
        validateNewsRequest(updateRequest);

        News news = newsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("News not found with id: " + id));

        if (news.getStatus() == NewsStatus.DELETED) {
            throw new IllegalStateException("Cannot update deleted news with id: " + id);
        }

        // Update basic fields
        news.setTitle(updateRequest.getTitle().trim());
        news.setContent(updateRequest.getContent().trim());

        // Update status if provided
        if (updateRequest.getNewsStatus() != null) {
            news.setStatus(updateRequest.getNewsStatus());
        }

        // Update thumbnail
        if (updateRequest.getThumbnailUrl() != null) {
            news.setThumbnailUrl(updateRequest.getThumbnailUrl().trim());
        }

        // Update media
        updateNewsMedia(news, updateRequest.getMediaUrls());

        News updatedNews = newsRepository.save(news);
        return newsMapper.toNewsDTO(updatedNews);
    }

    @Override
    @Transactional
    public void deleteNews(int id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("News not found with id: " + id));

        // Soft delete
        news.setStatus(NewsStatus.DELETED);
        newsRepository.save(news);
    }

    @Override
    public Page<NewsDTO> getAllNewsWithFilters(NewsStatus status, String title, Pageable pageable) {
        Page<News> newsPage = newsRepository.findAllWithFilters(status, title, pageable);
        return newsPage.map(newsMapper::toNewsDTO);
    }

    private void validateNewsRequest(CreateNewsRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Content is required");
        }

        if (request.getTitle().trim().length() > 255) {
            throw new IllegalArgumentException("Title must not exceed 255 characters");
        }
    }

    private List<NewsMedia> createNewsMediaList(List<String> mediaUrls, News news) {
        List<NewsMedia> newsMediaList = new ArrayList<>();

        for (String url : mediaUrls) {
            if (url != null && !url.trim().isEmpty()) {
                NewsMedia media = new NewsMedia();
                media.setMediaUrl(url.trim());
                media.setNews(news);
                media.setMediaType(detectMediaType(url));
                newsMediaList.add(media);
            }
        }

        return newsMediaList;
    }


    private MediaType detectMediaType(String url) {
        if (url == null || url.isEmpty()) {
            return MediaType.IMAGE;
        }

        String lowerUrl = url.toLowerCase();
        boolean isVideo = VIDEO_EXTENSIONS.stream()
                .anyMatch(lowerUrl::endsWith);

        return isVideo ? MediaType.VIDEO : MediaType.IMAGE;
    }

    private void updateNewsMedia(News news, List<String> newMediaUrls) {
        // Delete existing media
        List<NewsMedia> existingMedia = newsMediaRepository.findByNewsId(news.getId());
        if (!existingMedia.isEmpty()) {
            newsMediaRepository.deleteAll(existingMedia);
        }

        // Create new media
        if (newMediaUrls != null && !newMediaUrls.isEmpty()) {
            List<NewsMedia> newsMedia = createNewsMediaList(newMediaUrls, news);
            news.setNewsMedia(newsMedia);
        } else {
            news.setNewsMedia(new ArrayList<>());
        }
    }
}
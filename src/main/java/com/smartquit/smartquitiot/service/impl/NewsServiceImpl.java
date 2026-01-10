package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.document.NewsDocument;
import com.smartquit.smartquitiot.dto.request.CreateNewsRequest;
import com.smartquit.smartquitiot.dto.response.NewsDTO;
import com.smartquit.smartquitiot.entity.News;
import com.smartquit.smartquitiot.entity.NewsMedia;
import com.smartquit.smartquitiot.enums.MediaType;
import com.smartquit.smartquitiot.enums.NewsStatus;
import com.smartquit.smartquitiot.mapper.NewsMapper;
import com.smartquit.smartquitiot.repository.NewsMediaRepository;
import com.smartquit.smartquitiot.repository.NewsRepository;
import com.smartquit.smartquitiot.repository.NewsSearchRepository;
import com.smartquit.smartquitiot.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final NewsMediaRepository newsMediaRepository;
    private final NewsSearchRepository newsSearchRepository;

    private static final Set<String> VIDEO_EXTENSIONS = Set.of(".mp4", ".avi", ".mov", ".mkv", ".webm", ".flv");

    private NewsDocument mapToDocument(News news) {
        return NewsDocument.builder()
                .id(news.getId())
                .title(news.getTitle())
                .content(news.getContent())
                .status(news.getStatus().name())
                .thumbnailUrl(news.getThumbnailUrl())
                .createdAt(news.getCreatedAt())
                .build();
    }

    private NewsDTO mapDocumentToDTO(NewsDocument doc) {
        NewsDTO dto = new NewsDTO();
        dto.setId(doc.getId());
        dto.setTitle(doc.getTitle());
        dto.setContent(doc.getContent());
        dto.setThumbnailUrl(doc.getThumbnailUrl());
        try {
            dto.setStatus(String.valueOf(NewsStatus.valueOf(doc.getStatus())));
        } catch (Exception e) {
            dto.setStatus(String.valueOf(NewsStatus.DRAFT));
        }
        dto.setCreatedAt(String.valueOf(doc.getCreatedAt()));
        return dto;
    }

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
        if (query == null || query.isBlank()) {
            Page<News> newsPage = newsRepository.findByStatusOrderByCreatedAtDesc(
                    NewsStatus.PUBLISH,
                    PageRequest.of(0, Integer.MAX_VALUE)
            );
            return newsPage.getContent().stream()
                    .map(newsMapper::toNewsDTO)
                    .collect(Collectors.toList());
        } else {
            log.info("Searching news in ES with query: {}", query);
            List<NewsDocument> searchResults = newsSearchRepository.searchByKeyword(query.toLowerCase().trim());
            return searchResults.stream()
                    .map(this::mapDocumentToDTO)
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Cacheable(value = "news_details", key = "#id")
    public NewsDTO getNewsDetail(int id) {
        log.info("------- DB HIT: Fetching News Detail from Database for ID: {} (Redis Miss) -------", id);
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("News not found with id: " + id));
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
        News savedNews = newsRepository.save(news);
        if (createNewsRequest.getMediaUrls() != null && !createNewsRequest.getMediaUrls().isEmpty()) {
            List<NewsMedia> newsMedia = createNewsMediaList(createNewsRequest.getMediaUrls(), savedNews);
            savedNews.setNewsMedia(newsMedia);
            newsMediaRepository.saveAll(newsMedia);
        }
        try {
            newsSearchRepository.save(mapToDocument(savedNews));
        } catch (Exception e) {
            log.error("Failed to sync new news to Elasticsearch: {}", e.getMessage());
        }

        return newsMapper.toNewsDTO(savedNews);
    }

    @Override
    @Transactional
    @CacheEvict(value = "news_details", key = "#id")
    public NewsDTO updateNews(int id, CreateNewsRequest updateRequest) {
        log.info("------- CACHE EVICT: Updating News ID: {} (Removing from Redis) -------", id);
        validateNewsRequest(updateRequest);
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("News not found with id: " + id));
        if (news.getStatus() == NewsStatus.DELETED) {
            throw new IllegalStateException("Cannot update deleted news with id: " + id);
        }
        news.setTitle(updateRequest.getTitle().trim());
        news.setContent(updateRequest.getContent().trim());
        if (updateRequest.getNewsStatus() != null) {
            news.setStatus(updateRequest.getNewsStatus());
        }
        if (updateRequest.getThumbnailUrl() != null) {
            news.setThumbnailUrl(updateRequest.getThumbnailUrl().trim());
        }
        updateNewsMedia(news, updateRequest.getMediaUrls());
        News updatedNews = newsRepository.save(news);
        try {
            newsSearchRepository.save(mapToDocument(updatedNews));
        } catch (Exception e) {
            log.error("Failed to sync updated news to Elasticsearch: {}", e.getMessage());
        }
        return newsMapper.toNewsDTO(updatedNews);
    }


    @Override
    @Transactional
    @CacheEvict(value = "news_details", key = "#id")
    public void deleteNews(int id) {
        log.info("------- CACHE EVICT: Deleting News ID: {} (Removing from Redis) -------", id);
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("News not found with id: " + id));
        news.setStatus(NewsStatus.DELETED);
        newsRepository.save(news);
        try {
            newsSearchRepository.deleteById(id);
        } catch (Exception e) {
            log.error("Failed to delete news from Elasticsearch: {}", e.getMessage());
        }
    }

    @Override
    public Page<NewsDTO> getAllNewsWithFilters(NewsStatus status, String title, Pageable pageable) {
        Page<News> newsPage = newsRepository.findAllWithFilters(status, title, pageable);
        return newsPage.map(newsMapper::toNewsDTO);
    }


    @Override
    @Transactional
    public void syncAllNews() {
        log.info("Starting synchronization of all news to Elasticsearch...");
        List<News> allNews = newsRepository.findAll();
        List<NewsDocument> documents = allNews.stream()
                .map(this::mapToDocument)
                .collect(Collectors.toList());
        newsSearchRepository.saveAll(documents);
        log.info("Successfully synced {} news items to Elasticsearch", documents.size());
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
        if (url == null || url.isEmpty()) return MediaType.IMAGE;
        String lowerUrl = url.toLowerCase();
        boolean isVideo = VIDEO_EXTENSIONS.stream().anyMatch(lowerUrl::endsWith);
        return isVideo ? MediaType.VIDEO : MediaType.IMAGE;
    }

    private void updateNewsMedia(News news, List<String> newMediaUrls) {
        List<NewsMedia> existingMedia = newsMediaRepository.findByNewsId(news.getId());
        if (!existingMedia.isEmpty()) {
            newsMediaRepository.deleteAll(existingMedia);
        }
        if (newMediaUrls != null && !newMediaUrls.isEmpty()) {
            List<NewsMedia> newsMedia = createNewsMediaList(newMediaUrls, news);
            news.setNewsMedia(newsMedia);
            newsMediaRepository.saveAll(newsMedia);
        } else {
            news.setNewsMedia(new ArrayList<>());
        }
    }
}
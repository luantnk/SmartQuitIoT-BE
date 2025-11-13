package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.CreateNewsRequest;
import com.smartquit.smartquitiot.dto.response.NewsDTO;
import com.smartquit.smartquitiot.entity.News;
import com.smartquit.smartquitiot.entity.NewsMedia;
import com.smartquit.smartquitiot.enums.NewsStatus;
import com.smartquit.smartquitiot.mapper.NewsMapper;
import com.smartquit.smartquitiot.repository.NewsMediaRepository;
import com.smartquit.smartquitiot.repository.NewsRepository;
import com.smartquit.smartquitiot.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final NewsMediaRepository newsMediaRepository;


    @Override
    public List<NewsDTO> getLatestNews(int limit) {
        List<News> latest = newsRepository.findByStatusOrderByCreatedAtDesc(NewsStatus.PUBLISH,PageRequest.of(0, limit));
        return latest.stream()
                .map(newsMapper::toNewsDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<NewsDTO> getAllNews(String query) {
        List<News> news;
        if (query == null || query.isBlank()) {
            news = newsRepository.findByStatusOrderByCreatedAtDesc(NewsStatus.PUBLISH);
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
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));
        if( news.getStatus() == NewsStatus.DELETED ){
            throw new RuntimeException("News has been deleted");
        }
        return newsMapper.toNewsDTO(news);
    }


    @Override
    public NewsDTO createNews(CreateNewsRequest createNewsRequest) {
        News news = new News();
        news.setTitle(createNewsRequest.getTitle());
        news.setContent(createNewsRequest.getContent());
        news.setStatus(NewsStatus.PUBLISH);
        if( createNewsRequest.getMediaUrls() != null ){
            List<NewsMedia> newsMedia = new ArrayList<>();
            for (String url : createNewsRequest.getMediaUrls()) {
                NewsMedia media = new NewsMedia();
                media.setMediaUrl(url);
                media.setNews(news);
                if( url.endsWith(".mp4") || url.endsWith(".avi") || url.endsWith(".mov")){
                    media.setMediaType(com.smartquit.smartquitiot.enums.MediaType.VIDEO);
                } else {
                    media.setMediaType(com.smartquit.smartquitiot.enums.MediaType.IMAGE);
                }
                newsMedia.add(media);
            }
            news.setNewsMedia(newsMedia);
        }
        News savedNews = newsRepository.save(news);
        return newsMapper.toNewsDTO(savedNews);
    }

    @Override
    public NewsDTO updateNews(int id, CreateNewsRequest updateRequest) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));
        if( news.getStatus() == NewsStatus.DELETED ){
            throw new RuntimeException("Cannot update deleted news with id: " + id);
        }
        news.setTitle(updateRequest.getTitle());
        news.setContent(updateRequest.getContent());
        // Optionally update media if provided
        List<NewsMedia> existingMedia = newsMediaRepository.findByNewsId(news.getId());
        newsMediaRepository.deleteAll(existingMedia);
        if( updateRequest.getMediaUrls() != null ){
            List<NewsMedia> newsMedia = new ArrayList<>();
            for (String url : updateRequest.getMediaUrls()) {
                NewsMedia media = new NewsMedia();
                media.setMediaUrl(url);
                media.setNews(news);
                if( url.endsWith(".mp4") || url.endsWith(".avi") || url.endsWith(".mov")){
                    media.setMediaType(com.smartquit.smartquitiot.enums.MediaType.VIDEO);
                } else {
                    media.setMediaType(com.smartquit.smartquitiot.enums.MediaType.IMAGE);
                }
                newsMedia.add(media);
            }
            news.setNewsMedia(newsMedia);
        }else{
            news.setNewsMedia(new ArrayList<>());
        }
        News updatedNews = newsRepository.save(news);
        return newsMapper.toNewsDTO(updatedNews);
    }

    @Override
    public void deleteNews(int id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));
        news.setStatus(NewsStatus.DELETED);
        newsRepository.save(news);
    }
}

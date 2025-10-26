package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.CreateNewsRequest;
import com.smartquit.smartquitiot.dto.response.NewsDTO;
import com.smartquit.smartquitiot.entity.News;
import com.smartquit.smartquitiot.entity.NewsMedia;
import com.smartquit.smartquitiot.mapper.NewsMapper;
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

    @Override
    public List<NewsDTO> getLatestNews(int limit) {
        List<News> latest = newsRepository.findLatestNews(PageRequest.of(0, limit));
        return latest.stream()
                .map(newsMapper::toNewsDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<NewsDTO> getAllNews(String query) {
        List<News> news;
        if (query == null || query.isBlank()) {
            news = newsRepository.findAllByOrderByCreatedAtDesc();
        } else {
            news = newsRepository.searchNews(query.toLowerCase().trim());
        }
        return news.stream()
                .map(newsMapper::toNewsDTO)
                .collect(Collectors.toList());
    }

    @Override
    public NewsDTO createNews(CreateNewsRequest createNewsRequest) {
        News news = new News();
        news.setTitle(createNewsRequest.getTitle());
        news.setContent(createNewsRequest.getContent());
        news.setStatus(createNewsRequest.getStatus());
        if( createNewsRequest.getMediaUrls() != null ){
            List<NewsMedia> newsMedia = new ArrayList<>();
            for (String url : createNewsRequest.getMediaUrls()) {
                NewsMedia media = new NewsMedia();
                media.setMediaUrl(url);
                media.setNews(news);
                newsMedia.add(media);
            }
            news.setNewsMedia(newsMedia);
        }
        News savedNews = newsRepository.save(news);
        return newsMapper.toNewsDTO(savedNews);
    }
}

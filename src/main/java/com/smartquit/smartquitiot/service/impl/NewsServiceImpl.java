package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.NewsResponseDTO;
import com.smartquit.smartquitiot.entity.News;
import com.smartquit.smartquitiot.repository.NewsRepository;
import com.smartquit.smartquitiot.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;

    @Override
    public List<NewsResponseDTO> getLatestNews(int limit) {
        List<News> latest = newsRepository.findLatestNews(PageRequest.of(0, limit));
        return latest.stream()
                .map(NewsResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<NewsResponseDTO> getAllNews(String query) {
        List<News> news;
        if (query == null || query.isBlank()) {
            news = newsRepository.findAllByOrderByCreatedAtDesc();
        } else {
            news = newsRepository.searchNews(query.toLowerCase().trim());
        }
        return news.stream()
                .map(NewsResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}

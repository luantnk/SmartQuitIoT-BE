package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.CreateNewsRequest;
import com.smartquit.smartquitiot.dto.response.NewsDTO;
import com.smartquit.smartquitiot.enums.NewsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NewsService {

    List<NewsDTO> getLatestNews(int limit);

    List<NewsDTO> getAllNews(String query);

    NewsDTO getNewsDetail(int id);
    NewsDTO createNews(CreateNewsRequest createNewsRequest);

    NewsDTO updateNews(int id,CreateNewsRequest updateRequest);

    void deleteNews(int id);

    Page<NewsDTO> getAllNewsWithFilters(NewsStatus status, String title, Pageable pageable);
}

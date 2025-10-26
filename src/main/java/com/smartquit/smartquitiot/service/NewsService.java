package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.CreateNewsRequest;
import com.smartquit.smartquitiot.dto.response.NewsDTO;

import java.util.List;

public interface NewsService {

    List<NewsDTO> getLatestNews(int limit);

    List<NewsDTO> getAllNews(String query);

    NewsDTO createNews(CreateNewsRequest createNewsRequest);
}

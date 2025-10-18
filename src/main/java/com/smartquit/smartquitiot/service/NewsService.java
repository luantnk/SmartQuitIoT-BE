package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.response.NewsResponseDTO;

import java.util.List;

public interface NewsService {

    List<NewsResponseDTO> getLatestNews(int limit);

    List<NewsResponseDTO> getAllNews(String query);
}

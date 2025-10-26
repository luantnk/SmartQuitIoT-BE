package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.NewsDTO;
import com.smartquit.smartquitiot.entity.News;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewsMapper {

    private final NewsMediaMapper newsMediaMapper;

    public NewsDTO toNewsDTO(News news) {
        if (news == null) {
            return null;
        }
        NewsDTO dto = new NewsDTO();
        dto.setId(news.getId());
        dto.setTitle(news.getTitle());
        dto.setContent(news.getContent());
        dto.setStatus(news.getStatus().name());
        dto.setCreatedAt(news.getCreatedAt());
        dto.setMedia(newsMediaMapper.toNewsMediaDTO(news.getNewsMedia()));
        return dto;
    }

}

package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.NewsMediaDTO;
import com.smartquit.smartquitiot.entity.NewsMedia;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NewsMediaMapper {

    public NewsMediaDTO toNewsMediaDTO(NewsMedia newsMedia) {
        if (newsMedia == null) {
            return null;
        }
        NewsMediaDTO dto = new NewsMediaDTO();
        dto.setId(newsMedia.getId());
        dto.setMediaUrl(newsMedia.getMediaUrl());
        dto.setMediaType(newsMedia.getMediaType());
        return dto;
    }

    public List<NewsMediaDTO> toNewsMediaDTO(List<NewsMedia> newsMedia) {
        if (newsMedia == null) {
            return null;
        }
        return newsMedia.stream()
                .map(this::toNewsMediaDTO)
                .toList();
    }
}

package com.smartquit.smartquitiot.dto.response;

import com.smartquit.smartquitiot.entity.News;
import com.smartquit.smartquitiot.entity.NewsMedia;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsResponseDTO {

    private Integer id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private List<NewsMediaDTO> media;

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewsMediaDTO {
        private Integer id;
        private String mediaUrl;
    }

    public static NewsResponseDTO fromEntity(News news) {
        NewsResponseDTO dto = new NewsResponseDTO();
        dto.setId(news.getId());
        dto.setTitle(news.getTitle());
        dto.setContent(news.getContent());
        dto.setCreatedAt(news.getCreatedAt());
        if (news.getNewsMedia() != null) {
            dto.setMedia(news.getNewsMedia().stream()
                    .map(m -> new NewsMediaDTO(m.getId(), m.getMediaUrl()))
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}

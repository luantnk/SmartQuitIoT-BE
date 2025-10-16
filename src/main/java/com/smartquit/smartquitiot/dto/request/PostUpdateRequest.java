package com.smartquit.smartquitiot.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PostUpdateRequest {
    private String title;
    private String description;
    private String content;
    private String thumbnail;
    private List<PostMediaRequest> media;
    private Integer accountId; // 👈 thêm dòng này

    @Data
    public static class PostMediaRequest {
        private String mediaUrl;
        private String mediaType;
    }
}
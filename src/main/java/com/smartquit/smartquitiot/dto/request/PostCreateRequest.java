package com.smartquit.smartquitiot.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class PostCreateRequest {

    @NotBlank(message = "Title is required")
    String title;

    String description;

    @NotBlank(message = "Content is required")
    String content;

    String thumbnail;

    List<PostMediaRequest> media;

//    Integer accountId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PostMediaRequest {
        String mediaUrl;
        String mediaType; // IMAGE, VIDEO, AUDIO,...
    }
}

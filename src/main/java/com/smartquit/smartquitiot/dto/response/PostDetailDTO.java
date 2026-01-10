package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostDetailDTO {
    private Integer id;
    private String title;
    private String content;
    private String description;
    private String thumbnail;
    private String createdAt;
    private String updatedAt;
    private AccountDTO account;
    private List<PostMediaDTO> media;
    private List<CommentDTO> comments;
    private Integer commentCount;

    // ================= PostMediaDTO =================
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PostMediaDTO {
        private Integer id;
        private String mediaUrl;
        private String mediaType;
    }

    // ================= CommentDTO =================
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CommentDTO {
        private Integer id;
        private String content;
        private String createdAt;
        private AccountDTO account;
        private String avatarUrl;
        private List<PostMediaDTO> media;
        private List<CommentDTO> replies;
    }
}

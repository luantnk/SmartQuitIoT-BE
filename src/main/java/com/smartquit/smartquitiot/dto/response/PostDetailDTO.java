package com.smartquit.smartquitiot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostDetailDTO {
    private Integer id;
    private String title;
    private String content;
    private String description;
    private String thumbnail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AccountDTO account;
    private List<PostMediaDTO> media;
    private List<CommentDTO> comments;

    // ================= AccountDTO =================
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountDTO {
        private Integer id;
        private String username;
        private String firstName;
        private String email;
        private String lastName;
        private String avatarUrl;

        // 👇 thêm constructor tiện lợi 2 tham số
        public AccountDTO(Integer id, String username) {
            this.id = id;
            this.username = username;
        }
    }

    // ================= PostMediaDTO =================
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostMediaDTO {
        private Integer id;
        private String mediaUrl;
        private String mediaType;
    }

    // ================= CommentDTO =================
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentDTO {
        private Integer id;
        private String content;
        private String createdAt;
        private AccountDTO account;
        private List<PostMediaDTO> media;
        private List<CommentDTO> replies;
    }
}

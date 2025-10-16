package com.smartquit.smartquitiot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostSummaryDTO {
    private Integer id;
    private String title;
    private String description;
    private String thumbnail;
    private String createdAt;
    private AccountDTO account;

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountDTO {
        private Integer id;
        private String username;
        private String firstName;
        private String lastName;
        private String avatarUrl;
    }

}

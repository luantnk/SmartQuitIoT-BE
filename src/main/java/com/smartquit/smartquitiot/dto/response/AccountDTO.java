package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountDTO {

    Integer id;
    String username;
    String email;
    String role;
    String accountType;
    Boolean isActive;
    Boolean isBanned;
    Boolean isFirstLogin;
    LocalDateTime createdAt;

    String firstName;
    String lastName;
    String avatarUrl;
}

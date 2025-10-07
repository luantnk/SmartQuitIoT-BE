package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CoachDTO {

    int id;
    String email;
    String firstName;
    String lastName;
    String avatarUrl;
    String gender;
    String certificateUrl;
    String bio;
    int ratingCount;
    double ratingAvg;
    AccountDTO account;
}

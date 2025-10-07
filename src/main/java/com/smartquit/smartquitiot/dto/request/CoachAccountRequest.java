package com.smartquit.smartquitiot.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoachAccountRequest {

    String username;
    String password;
    String email;
    String firstName;
    String lastName;
    String gender;
    String certificateUrl;
    int experienceYears;
    String specializations;
}

package com.smartquit.smartquitiot.dto.request;

import com.smartquit.smartquitiot.enums.Gender;
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
    String confirmPassword;
    String email;
    String firstName;
    String lastName;
    Gender gender;
    String certificateUrl;
    int experienceYears;
    String specializations;
}

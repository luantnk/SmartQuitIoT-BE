package com.smartquit.smartquitiot.dto.request;

import com.smartquit.smartquitiot.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoachAccountRequest {

    @NotEmpty(message = "Username can not empty")
    String username;
    @NotEmpty(message = "Password can not empty")
    String password;
    @NotEmpty(message = "Confirm password can not empty")
    String confirmPassword;
    @NotEmpty(message = "Email can not empty")
    String email;
    @NotEmpty(message = "Firstname can not empty")
    String firstName;
    @NotEmpty(message = "Lastname can not empty")
    String lastName;
    Gender gender;
    String certificateUrl;
    @Positive
    @Min(value = 1,message = "Coach experience must greater than 0")
    @Max(value = 50, message = "Invalid number of experience year")
    int experienceYears;
    String specializations;
}

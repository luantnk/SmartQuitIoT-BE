package com.smartquit.smartquitiot.dto.request;

import com.smartquit.smartquitiot.enums.Gender;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MemberAccountRequest {

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
    @NotEmpty(message = "Gender can not empty")
    Gender gender;
    LocalDate dob;
}

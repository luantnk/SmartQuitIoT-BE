package com.smartquit.smartquitiot.dto.request;

import com.smartquit.smartquitiot.enums.Gender;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MemberAccountRequest {

    String username;
    String password;
    String confirmPassword;
    String email;
    String firstName;
    String lastName;
    Gender gender;
    LocalDate dob;
}

package com.smartquit.smartquitiot.dto.request;

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
    String email;
    String firstName;
    String lastName;
    String gender;
    LocalDate dob;
}

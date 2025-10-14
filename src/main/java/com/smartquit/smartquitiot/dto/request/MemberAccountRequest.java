package com.smartquit.smartquitiot.dto.request;

import com.smartquit.smartquitiot.enums.Gender;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
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
    LocalDate dob;
}

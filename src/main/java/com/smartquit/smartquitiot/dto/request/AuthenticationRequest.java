package com.smartquit.smartquitiot.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationRequest {

    @NotEmpty(message = "Invalid username/email or password")
    String usernameOrEmail;
    @NotEmpty(message = "Invalid username/email or password")
    String password;

}

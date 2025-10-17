package com.smartquit.smartquitiot.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoachUpdateRequest {
    @NotEmpty(message = "FirstName is required")
    String firstName;
    @NotEmpty(message = "LastName is required")
    String lastName;
    String avatarUrl;
    String certificateUrl;
    String bio;
    int experienceYears;
    String specializations;
}

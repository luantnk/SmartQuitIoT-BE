package com.smartquit.smartquitiot.dto.request;

import com.smartquit.smartquitiot.validator.DobConstraint;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MemberUpdateRequest {
    @NotEmpty(message = "FirstName is required")
    String firstName;
    @NotEmpty(message = "LastName is required")
    String lastName;
    @DobConstraint(min = 14, message = "Member must greater than 14 years old")
    LocalDate dob;
    String avatarUrl;
}

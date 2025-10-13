package com.smartquit.smartquitiot.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoachSummaryDTO {
    Integer id;
    String firstName;
    String lastName;
    String avatarUrl;
    Double ratingAvg;
}

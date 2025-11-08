package com.smartquit.smartquitiot.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateNewQuitPlanRequest {
    LocalDate startDate;
    boolean useNRT;
    String quitPlanName;
}

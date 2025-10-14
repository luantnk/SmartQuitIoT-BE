package com.smartquit.smartquitiot.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScheduleAssignRequest {

    @NotEmpty(message = "dates must not be empty")
    @JsonFormat(pattern = "yyyy-MM-dd")
    List<
            @NotNull(message = "date must not be null")
            @FutureOrPresent(message = "date must be today or in the future")
                    LocalDate
            > dates;

    @NotEmpty(message = "coachIds must not be empty")
    List<@NotNull @Positive Integer> coachIds;
}

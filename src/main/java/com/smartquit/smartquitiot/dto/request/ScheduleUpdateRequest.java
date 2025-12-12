package com.smartquit.smartquitiot.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScheduleUpdateRequest {

    @NotNull
    List<Integer> addCoachIds;

    @NotNull
    List<Integer> removeCoachIds;
}

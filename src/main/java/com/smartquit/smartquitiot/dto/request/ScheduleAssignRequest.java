package com.smartquit.smartquitiot.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScheduleAssignRequest {
    List<String> dates;
    List<Integer> coachIds;
}

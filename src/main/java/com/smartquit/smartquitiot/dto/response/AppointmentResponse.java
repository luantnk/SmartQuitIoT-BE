package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppointmentResponse {
    int appointmentId;
    int coachId;
    String coachName;
    int slotId;
    LocalDate date;
    LocalTime startTime;
    LocalTime endTime;
    String runtimeStatus;
}

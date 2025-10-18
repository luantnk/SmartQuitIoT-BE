package com.smartquit.smartquitiot.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppointmentRequest {
    @NotNull(message = "Member ID is required")
    int memberId;
    @NotNull(message = "Coach ID is required")
    int coachId;
    @NotNull(message = "Slot ID is required")
    int slotId;
    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "Date must be today or in the future")
    LocalDate date;
}

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
    @NotNull(message = "Coach ID is required")
    int coachId;

    @NotNull(message = "slot ID is required")
    int slotId;

    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "Date must be today or in the future")
    LocalDate date;

    /**
     * Nếu true, cho phép đặt lịch ngay cả khi trùng thời gian với appointment khác (cùng member, cùng slot, khác coach).
     * Nếu false hoặc null, sẽ throw exception cảnh báo khi phát hiện trùng thời gian.
     */
    Boolean forceConfirm;
}

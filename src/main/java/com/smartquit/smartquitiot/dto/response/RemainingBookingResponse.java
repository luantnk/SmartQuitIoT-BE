package com.smartquit.smartquitiot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemainingBookingResponse {
    // Tổng lượt được phép trong kỳ subscription (theo luật: 4 lượt / 30 ngày)
    private int allowed;

    // Số lượt đã dùng trong kỳ (đếm appointment non-cancelled
    // hoặc cancelledBy = MEMBER)
    private int used;

    // Còn lại = max(0, allowed - used)
    private int remaining;

    // Bắt đầu kỳ subscription
    private LocalDate periodStart;

    // Kết thúc kỳ subscription
    private LocalDate periodEnd;

    private String note;
}

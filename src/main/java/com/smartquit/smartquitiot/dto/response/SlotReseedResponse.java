package com.smartquit.smartquitiot.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SlotReseedResponse {
    int createdCount;    // Số slots mới được tạo
    int deletedCount;    // Số orphan slots bị xóa
    int totalSlots;      // Tổng số slots hiện tại
}


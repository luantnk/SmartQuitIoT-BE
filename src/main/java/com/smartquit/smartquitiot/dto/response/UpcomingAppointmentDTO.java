package com.smartquit.smartquitiot.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpcomingAppointmentDTO {
    int appointmentId;
    int memberId;
    String memberName;
    String memberAvatarUrl;
    String time; // format: "HH:mm"
    String status; // "pending", "completed", etc.
}

package com.smartquit.smartquitiot.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FeedbackResponse {
    private int id; // id Feedback
    private String memberName;
    private String avatarUrl;
    private LocalDateTime date; // ngày tạo (gửi feedback)
    private String content;
    private int rating; // tương đương star
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
}

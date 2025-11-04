package com.smartquit.smartquitiot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantSummaryDTO {
    private int id;
    private String fullName;
    private String avatarUrl;
}

package com.smartquit.smartquitiot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MissionPlanDTO {
    private String code;
    private String title;
    private String description;
}

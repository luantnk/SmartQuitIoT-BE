package com.smartquit.smartquitiot.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.enums.MissionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMissionRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Mission Phase is required")
    private MissionPhase phase;

    @NotNull(message = "Status is required")
    private MissionStatus status;

    @Min(value = 0, message = "EXP must be at least 0")
    private int exp;

    @NotNull(message = "Mission type ID is required")
    private Integer missionTypeId;

    private Integer interestCategoryId; // nullable

    private String condition; // JSON string
}

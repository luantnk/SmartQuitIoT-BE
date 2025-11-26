package com.smartquit.smartquitiot.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAchievementRequest {
    @NotBlank(message = "Achievement name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Icon URL is required")
    private String icon;

    @NotBlank(message = "Achievement type is required")
    private String type; // STREAK, ACTIVITY, FINANCE, SOCIAL, PROGRESS

    @NotNull(message = "Condition is required")
    private JsonNode condition;
}

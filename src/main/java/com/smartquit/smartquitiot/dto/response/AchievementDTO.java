package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AchievementDTO {
     int id;
     String name;
     String description;
     String icon;
     String type;
     JsonNode condition;
     LocalDateTime achievedAt;
     LocalDateTime createdAt;
     LocalDateTime updatedAt;
     boolean unlocked;
}

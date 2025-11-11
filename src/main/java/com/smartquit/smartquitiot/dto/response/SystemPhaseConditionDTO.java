package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SystemPhaseConditionDTO {
    int id;
    String name;
    JsonNode condition;
    LocalDateTime updatedAt;
}

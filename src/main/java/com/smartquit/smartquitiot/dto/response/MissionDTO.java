package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MissionDTO {

    Integer id;
    String code;
    String name;
    String description;
    String phase;
    String status;
    JsonNode condition;
    Integer exp;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    MissionTypeDTO missionType;
    InterestCategoryDTO interestCategory;
}

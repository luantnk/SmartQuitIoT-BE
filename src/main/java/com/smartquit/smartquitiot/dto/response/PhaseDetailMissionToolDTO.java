package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.enums.MissionStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhaseDetailMissionDTO {
    int id;
    String code;
    String name;
    String description;
    MissionPhase missionPhase;
    MissionStatus missionStatus;
    int exp;
    JsonNode condition;
    MissionTypeDTO missionTypeDTO;
    InterestCategoryDTO interestCategoryDTO;
}

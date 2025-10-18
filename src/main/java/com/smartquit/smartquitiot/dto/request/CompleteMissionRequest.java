package com.smartquit.smartquitiot.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompleteMissionRequest {
    int phaseId;
    int phaseDetailMissionId;
    JsonNode answer;
    String notes;
}

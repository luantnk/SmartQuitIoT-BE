package com.smartquit.smartquitiot.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompleteMissionRequest {
    int phaseId;
    int phaseDetailMissionId;
    List<String> triggered;
    String notes;
}

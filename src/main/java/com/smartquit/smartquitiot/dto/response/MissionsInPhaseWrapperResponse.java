package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartquit.smartquitiot.enums.PhaseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MissionsInPhaseWrapperResponse {
    private LocalDate currentDate;
    private Integer phaseId;
    private String name;
    private PhaseStatus status;
    private List<MissionInPhaseResponse> items;
}

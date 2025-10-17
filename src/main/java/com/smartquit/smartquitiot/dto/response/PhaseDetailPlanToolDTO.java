package com.smartquit.smartquitiot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhaseDetailPlanToolDTO {
    private Integer phaseDetailId;
    String phaseDetailName;
    LocalDate date;
    int dayIndex;
    private List<PhaseDetailMissionPlanToolDTO> missions;
}

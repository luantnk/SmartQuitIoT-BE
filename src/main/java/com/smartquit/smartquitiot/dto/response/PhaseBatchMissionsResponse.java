package com.smartquit.smartquitiot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhaseBatchMissionsResponse {
    private Integer phaseId;
    private String phaseName;
    private int durationDays;
    private List<PhaseDetailPlanToolDTO> items;
}
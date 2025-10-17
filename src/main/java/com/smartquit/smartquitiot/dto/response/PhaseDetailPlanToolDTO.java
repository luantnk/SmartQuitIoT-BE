package com.smartquit.smartquitiot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailPlanDTO {
    private Long phaseDetailId;
    private List<MissionPlanDTO> missions;
}

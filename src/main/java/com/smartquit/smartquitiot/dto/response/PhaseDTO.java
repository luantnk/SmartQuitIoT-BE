package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.smartquit.smartquitiot.enums.PhaseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhaseDTO {
    private int id;
    private String name;
    private LocalDate startDateOfQuitPlan; // cái này ở home page figma phần đầu tiên có time đếm ngược, nhét vào đây cho đở tốn API
    private LocalDate startDate;
    private LocalDate endDate;
    private int durationDay;
    private String reason;
    private PhaseStatus status;
    private LocalDateTime completedAt;
    private LocalDateTime createAt;
    private boolean keepPhase;
    private boolean redo;
    int totalMissions;
    int completedMissions;
//    BigDecimal progress;
//    double avg_craving_level;
//    double avg_cigarettes;
//    double fm_cigarettes_total;
     SnapshotMetricDTO snapshotMetricDTO;
    JsonNode condition;
    List<PhaseDetailResponseDTO> details;
    PhaseDetailResponseDTO currentPhaseDetail;

}

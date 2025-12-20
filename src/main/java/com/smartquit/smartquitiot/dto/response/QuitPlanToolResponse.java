package com.smartquit.smartquitiot.dto.response;

import com.smartquit.smartquitiot.enums.QuitPlanStatus;
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
public class QuitPlanToolResponse {
    int id;
    String name;
    QuitPlanStatus status;
    LocalDate startDate;
    LocalDate endDate;
    LocalDateTime createdAt;
    boolean useNRT;
    boolean active;
    int ftndScore;
    FormMetricDTO formMetricDTO;
   // CurrentMetricDTO currentMetricDTO;
    PhaseDTO currentPhaseDTO;

}

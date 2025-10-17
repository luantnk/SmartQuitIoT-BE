package com.smartquit.smartquitiot.dto.response;

import com.smartquit.smartquitiot.enums.QuitPlanStatus;
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
public class QuitPlanResponse {
    int id;
    String name;
    QuitPlanStatus status;
    LocalDate startDate;
    LocalDate endDate;
    boolean useNRT;
    int ftndScore;
    FormMetricDTO formMetricDTO;
    List<PhaseDTO> phases;
}

package com.smartquit.smartquitiot.dto.response;

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
public class PhaseResponse {
    List<PhaseDTO> phases;
    LocalDate startDateOfQuitPlan;
    LocalDate endDateOfQuitPlan;
}


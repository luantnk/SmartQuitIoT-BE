package com.smartquit.smartquitiot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PhaseDTO {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private int durationDay;
    private String reason;
}

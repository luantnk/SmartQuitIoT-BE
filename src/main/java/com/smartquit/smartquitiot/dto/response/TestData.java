package com.smartquit.smartquitiot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TestData {

    // Phase data
    private Double progress;              // từ phase.getProgress()

    // Member metric data
    private Double avgCravingLevel;       // từ member.metric.avgCravingLevel
    private Double avgCigarettesPerDay;   // từ member.metric.avgCigarettesPerDay
    private Double avgMood;               // từ member.metric.avgMood
    private Double avgAnxiety;            // từ member.metric.avgAnxiety
    private Double avgConfidentLevel;     // từ member.metric.avgConfidentLevel

    // Form metric data
    private Double fmCigarettesTotal;     // từ formMetric.smokeAvgPerDay
}

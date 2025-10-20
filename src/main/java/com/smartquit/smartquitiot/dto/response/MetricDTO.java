package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricDTO {
    //show at home screen dashboard
    Integer streaks;
    BigDecimal annualSaved;
    BigDecimal moneySaved;
    Double reductionPercentage;
    Double smokeFreeDayPercentage;

    //show at health data screen
    //avg metrics
    Double avgCravingLevel;
    Double avgMood;
    Double avgAnxiety;
    Double avgConfidentLevel;
    Integer avgCigarettesPerDay;

    //current metrics
    Integer currentCravingLevel;
    Integer currentMoodLevel;
    Integer currentConfidenceLevel;
    Integer currentAnxietyLevel;

    //iot metrics
    Integer steps;
    Integer heartRate;
    Integer spo2;
    Integer activityMinutes;
    Integer respiratoryRate;
    Double sleepDuration;
    Integer sleepQuality;


}

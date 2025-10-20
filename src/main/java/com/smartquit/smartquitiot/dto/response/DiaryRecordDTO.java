package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiaryRecordDTO {

    Integer id;
    LocalDate date;
    Boolean haveSmoked;
    Integer cigarettesSmoked;
    List<String> triggers;
    Boolean isUseNrt;
    Double moneySpentOnNrt;
    Integer cravingLevel;
    Integer moodLevel;
    Integer confidenceLevel;
    Integer anxietyLevel;
    String note;
    Boolean isConnectIoTDevice;
    Integer steps;
    Integer heartRate;
    Integer spo2;
    Integer activityMinutes;
    Integer respiratoryRate;
    Double sleepDuration;
    Integer sleepQuality;
    BigDecimal estimatedNicotineIntake;
    Double reductionPercentage;
}

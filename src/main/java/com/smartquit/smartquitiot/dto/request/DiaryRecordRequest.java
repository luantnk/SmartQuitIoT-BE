package com.smartquit.smartquitiot.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiaryRecordRequest {

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
}

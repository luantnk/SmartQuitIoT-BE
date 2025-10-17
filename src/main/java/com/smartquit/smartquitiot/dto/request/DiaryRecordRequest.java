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
    boolean haveSmoked;
    int cigarettesSmoked;
    List<String> triggers;
    boolean isUseNrt;
    double moneySpentOnNrt;
    int cravingLevel;
    int moodLevel;
    int confidenceLevel;
    int anxietyLevel;
    String note;
    boolean isConnectIoTDevice;
    int steps;
    int heartRate;
    int spo2;
    int activityMinutes;
    int respiratoryRate;
    double sleepDuration;
    int sleepQuality;
}

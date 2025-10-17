package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiaryRecordDTO {

    int id;
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

package com.smartquit.smartquitiot.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
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
    @Min(value = 0, message = "Invalid cigarettes smoked number")
    Integer cigarettesSmoked;
    List<String> triggers;
    Boolean isUseNrt;
    @Min(value = 0, message = "Invalid money spent on nrt")
    Double moneySpentOnNrt;
    @Positive(message = "Invalid craving level")
    @Max(value = 10, message = "Invalid craving level")
    @Min(value = 1, message = "Invalid craving level")
    Integer cravingLevel;
    @Max(value = 10, message = "Invalid mood level")
    @Min(value = 1, message = "Invalid mood level")
    Integer moodLevel;
    @Max(value = 10, message = "Invalid confidence level")
    @Min(value = 1, message = "Invalid confidence level")
    Integer confidenceLevel;
    @Max(value = 10, message = "Invalid anxiety level")
    @Min(value = 1, message = "Invalid anxiety level")
    Integer anxietyLevel;
    String note;
    Boolean isConnectIoTDevice;
    @Min(value = 0, message = "Invalid steps count")
    Integer steps;
    @Min(value = 0, message = "Invalid heart rate")
    Integer heartRate;
    @Min(value = 0, message = "Invalid spo2 level")
    Integer spo2;
    @Min(value = 0, message = "Invalid activity minutes")
    Integer activityMinutes;
    @Min(value = 0, message = "Invalid respiratory rate")
    Integer respiratoryRate;
    @Min(value = 0, message = "Invalid sleep duration")
    Double sleepDuration;
    @Min(value = 0, message = "Invalid sleep quality")
    Integer sleepQuality;
}

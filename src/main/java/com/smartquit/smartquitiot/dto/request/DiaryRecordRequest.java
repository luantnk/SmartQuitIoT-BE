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
    @Max(value = 50, message = "Nooo..., you smoked too much!")
    @Min(value = 0, message = "Invalid cigarettes smoked number")
    Integer cigarettesSmoked;
    List<String> triggers;
    Boolean isUseNrt;
    @Min(value = 0, message = "Invalid money spent on nrt")
    Double moneySpentOnNrt;
    @Max(value = 10, message = "Invalid craving level, max range is 10")
    @Min(value = 1, message = "Invalid craving level, min range is 1")
    Integer cravingLevel;
    @Max(value = 10, message = "Invalid mood level, max range is 10")
    @Min(value = 1, message = "Invalid mood level, min range is 1")
    Integer moodLevel;
    @Max(value = 10, message = "Invalid confidence level, max range is 10")
    @Min(value = 1, message = "Invalid confidence level, min range is 1")
    Integer confidenceLevel;
    @Max(value = 10, message = "Invalid anxiety level, max range is 10")
    @Min(value = 1, message = "Invalid anxiety level, min range is 1")
    Integer anxietyLevel;
    String note;
    Boolean isConnectIoTDevice;
    @Min(value = 0, message = "Invalid steps count")
    Integer steps;
    @Max(value = 300, message = "It is not possible for a human to have such a high heart rate.")
    @Min(value = 0, message = "Invalid heart rate")
    Integer heartRate;
    @Max(value = 100, message = "Invalid spo2 level")
    @Min(value = 0, message = "Invalid spo2 level")
    Integer spo2;
    @Min(value = 0, message = "Invalid activity minutes")
    Integer activityMinutes;
    @Min(value = 0, message = "Invalid respiratory rate")
    Integer respiratoryRate;
    @Min(value = 0, message = "Invalid sleep duration")
    Double sleepDuration;
    @Max(value = 100, message = "Invalid sleep quality")
    @Min(value = 0, message = "Invalid sleep quality")
    Integer sleepQuality;
}

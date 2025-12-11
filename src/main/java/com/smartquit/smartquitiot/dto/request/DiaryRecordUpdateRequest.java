package com.smartquit.smartquitiot.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiaryRecordUpdateRequest {

    Integer cigarettesSmoked;
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
}

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
public class SnapshotMetricDTO {
    //gia tri khi no pass
    BigDecimal progress;
    double avgCravingLevel;
    double avgCigarettesPerDay;
    double avgMood;
    double avgAnxiety;
    double avgConfidentLevel;
}


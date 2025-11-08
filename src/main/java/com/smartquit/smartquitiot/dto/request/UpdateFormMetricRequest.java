package com.smartquit.smartquitiot.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateFormMetricRequest {
    int smokeAvgPerDay;
    int numberOfYearsOfSmoking;
    int cigarettesPerPackage;
    int minutesAfterWakingToSmoke;
    boolean smokingInForbiddenPlaces = false;
    boolean cigaretteHateToGiveUp = false;
    boolean morningSmokingFrequency = false;
    boolean smokeWhenSick = false;
    BigDecimal moneyPerPackage;
    BigDecimal estimatedMoneySavedOnPlan;
    BigDecimal amountOfNicotinePerCigarettes;
    BigDecimal estimatedNicotineIntakePerDay;
    List<String> interests;
    List<String> triggered;// xem o create quit plan request
}

package com.smartquit.smartquitiot.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateQuitPlanInFirstLoginRequest {
    //quit plan
    LocalDate startDate;
    boolean useNRT;
    String quitPlanName;
    //form metric
    int smokeAvgPerDay;
    int numberOfYearsOfSmoking;
    BigDecimal moneyPerPackage;
    int cigarettesPerPackage;
    int minutesAfterWakingToSmoke;
    boolean smokingInForbiddenPlaces;
    boolean cigaretteHateToGiveUp;
    boolean morningSmokingFrequency;
    boolean smokeWhenSick;
    List<String> interests;
    BigDecimal amountOfNicotinePerCigarettes;
}

package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormMetricDTO {

    int id;
    int smokeAvgPerDay; //base
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

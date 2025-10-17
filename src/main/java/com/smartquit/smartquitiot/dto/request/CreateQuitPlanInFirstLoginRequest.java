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
    List<String> interests; // la select option lay trong Interest category
    BigDecimal amountOfNicotinePerCigarettes;
 //   List<String> triggered;  cai nay se dem vao lam mission
 //const TRIGGERS = [
    //  "Morning",
    //  "After Meal",
    //  "Gaming",
    //  "Party",
    //  "Coffee",
    //  "Stress",
    //  "Boredom",
    //  "Driving",
    //  "Sadness",
    //  "Work"
    //]; -- do khong luu vao 1 bang cu the nen tren UI se luu cac cai nay de cho nguoi ta chon  khi thuc hien nhiem vu



}

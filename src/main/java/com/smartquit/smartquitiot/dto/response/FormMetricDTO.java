package com.smartquit.smartquitiot.dto.response;

import com.smartquit.smartquitiot.entity.QuitPlan;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FormMetricDTO {

    int id;
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

package com.smartquit.smartquitiot.entity;


import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FormMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
   // String reason;

    @Column(name = "interests", columnDefinition = "json")
    @Type(JsonType.class)
    List<String> interests;

    @Column(name = "triggered", columnDefinition = "json")
    @Type(JsonType.class)
    List<String> triggered;// xem o create quit plan request

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    LocalDateTime updatedAt;

    @OneToOne(mappedBy = "formMetric")
    QuitPlan quitPlan;


}

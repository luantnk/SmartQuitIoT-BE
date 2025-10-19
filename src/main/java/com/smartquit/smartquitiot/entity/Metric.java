package com.smartquit.smartquitiot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartquit.smartquitiot.enums.MetricType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Metric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    int streaks;
    int relapseCountInPhase = 0;

    //avg metrics
    double avgCravingLevel;
    double avgMood;
    double avgAnxiety;
    double avgConfidentLevel;

    //current metrics
    int currentCravingLevel;
    int currentMoodLevel;
    int currentConfidenceLevel;
    int currentAnxietyLevel;

    //iot metrics
    int steps;
    int heartRate;
    int spo2;
    int activityMinutes;
    int respiratoryRate;
    double sleepDuration;
    int sleepQuality;

    //money metrics
    BigDecimal annualSaved;
    BigDecimal moneySaved;

    @CreationTimestamp
    LocalDateTime createdAt;
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "member_id")
    Member member;

}

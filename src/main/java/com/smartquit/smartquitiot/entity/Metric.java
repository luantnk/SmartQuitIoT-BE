package com.smartquit.smartquitiot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    int streaks = 0;
    int relapseCountInPhase = 0;
    int post_count = 0;
    int comment_count = 0;
    int total_mission_completed = 0;
    int completed_all_mission_in_day = 0;

    //avg metrics
    double avgCravingLevel;
    double avgMood;
    double avgAnxiety;
    double avgConfidentLevel;
    double avgCigarettesPerDay;

    //current metrics
    int currentCravingLevel;
    int currentMoodLevel;
    int currentConfidenceLevel;
    int currentAnxietyLevel;

    //iot metrics
    int steps;
    int heartRate;
    int spo2;
    double sleepDuration;

    //money metrics
    BigDecimal annualSaved;
    BigDecimal moneySaved;

    //smokeMetrics
    double reductionPercentage;
    double smokeFreeDayPercentage;

    @CreationTimestamp
    LocalDateTime createdAt;
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "member_id")
    Member member;

}

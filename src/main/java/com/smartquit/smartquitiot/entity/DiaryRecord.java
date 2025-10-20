package com.smartquit.smartquitiot.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiaryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    LocalDate date;
    boolean haveSmoked;
    int cigarettesSmoked;
    List<String> triggers;
    boolean isUseNrt;
    double moneySpentOnNrt;
    int cravingLevel;
    int moodLevel;
    int confidenceLevel;
    int anxietyLevel;
    String note;
    // new attribute, new metric, can use for showing dashboard
    boolean isConnectIoTDevice;
    int steps;
    int heartRate;
    int spo2;
    int activityMinutes;
    int respiratoryRate;
    double sleepDuration;
    int sleepQuality;
    BigDecimal estimatedNicotineIntake;
    double reductionPercentage;

    @CreationTimestamp
    LocalDateTime createdAt;
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    Member member;

}

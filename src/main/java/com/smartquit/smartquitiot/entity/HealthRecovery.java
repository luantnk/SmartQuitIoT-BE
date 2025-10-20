package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.HealthRecoveryDataName;
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
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HealthRecovery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    @Column(unique = true)
    @Enumerated(EnumType.STRING)
    HealthRecoveryDataName name;
    @Column(precision = 5, scale = 2)
    BigDecimal value;
    String description;
    LocalDateTime timeTriggered;//thời gian bắt đầu
    LocalDateTime timeStarted;
    int recoveryTime;//thời gian để hồi phục đơn vị là phút

    @ManyToOne(fetch = FetchType.LAZY)
    Member member;
}


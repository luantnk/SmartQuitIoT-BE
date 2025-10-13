package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.Operator;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemRuleMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    String field;
    @Enumerated(EnumType.STRING)
    Operator operator;
    double value;
    @CreationTimestamp
    LocalDateTime createdAt;
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    Mission mission;

}

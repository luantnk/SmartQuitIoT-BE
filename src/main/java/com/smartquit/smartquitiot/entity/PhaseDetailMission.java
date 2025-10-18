package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.PhaseDetailMissionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PhaseDetailMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    String code;
    String name;
    String description;
    LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    PhaseDetailMissionStatus  status = PhaseDetailMissionStatus.INCOMPLETED;

    @ManyToOne(fetch = FetchType.LAZY)
    PhaseDetail phaseDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    Mission mission;
}

package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.PhaseDetailMissionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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

    @Enumerated(EnumType.STRING)
    PhaseDetailMissionStatus  status = PhaseDetailMissionStatus.INCOMPLETED;

    @ManyToOne(fetch = FetchType.LAZY)
    PhaseDetail phaseDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    Mission mission;
}

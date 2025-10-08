package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.enums.MissionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    @Column(unique = true)
    String code;
    String name;
    String description;
    @Enumerated(EnumType.STRING)
    MissionPhase phase;
    @Enumerated(EnumType.STRING)
    MissionStatus status;
    int exp;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    MissionType missionType;

    @ManyToOne(fetch = FetchType.LAZY)
    InterestCategory interestCategory;

    @OneToMany(mappedBy = "mission")
    List<SystemRuleMission> systemRuleMissions;

    @OneToMany(mappedBy = "mission")
    List<PhaseDetailMission> phaseDetailMissions;

}

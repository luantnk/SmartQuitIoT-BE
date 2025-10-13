package com.smartquit.smartquitiot.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.enums.MissionStatus;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;

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

    @Column(name = "condition_json", columnDefinition = "JSON")
    @Type(JsonType.class)
    private JsonNode condition;

    @ManyToOne(fetch = FetchType.LAZY)
    MissionType missionType;

    @ManyToOne(fetch = FetchType.LAZY)
    InterestCategory interestCategory;

    @OneToMany(mappedBy = "mission")
    List<SystemRuleMission> systemRuleMissions;

    @OneToMany(mappedBy = "mission")
    List<PhaseDetailMission> phaseDetailMissions;

}

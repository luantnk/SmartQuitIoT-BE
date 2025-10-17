package com.smartquit.smartquitiot.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartquit.smartquitiot.enums.PhaseStatus;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "phase")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Phase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    String name;
    LocalDate startDate;
    LocalDate endDate;
    int durationDays;

    @Column(name = "condition_json", columnDefinition = "JSON", nullable = false)
    @Type(JsonType.class)
    JsonNode condition;

    String reason;

    @Enumerated(EnumType.STRING)
    PhaseStatus status;

    @CreationTimestamp
    @Column(nullable = false)
    LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quit_plan_id")
    QuitPlan quitPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_phase_condition_id")
    SystemPhaseCondition systemPhaseCondition;

    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PhaseDetail> details = new ArrayList<>();


}

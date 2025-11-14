package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.PhaseEnum;
import com.smartquit.smartquitiot.enums.ReminderType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReminderTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Enumerated(EnumType.STRING)
    PhaseEnum phaseEnum;

    @Enumerated(EnumType.STRING)
    ReminderType reminderType;

    @Column(columnDefinition = "TEXT")
    String content;

//    @Type(JsonType.class)
//    @Column(name = "rule_condition_json", columnDefinition = "JSON", nullable = false)
//    JsonNode ruleCondition;
    String triggerCode;

    @CreationTimestamp
    LocalDateTime createdAt;
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "reminderTemplate")
    List<ReminderQueue> reminderQueues;

}

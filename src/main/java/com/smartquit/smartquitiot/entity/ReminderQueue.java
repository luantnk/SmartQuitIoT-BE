package com.smartquit.smartquitiot.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartquit.smartquitiot.enums.ReminderQueueStatus;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReminderQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(columnDefinition = "TEXT")
    String content;

//    @Type(JsonType.class)
//    @Column(name = "payload_json", columnDefinition = "JSON", nullable = false)
//    JsonNode payload;

    @Enumerated(EnumType.STRING)
    ReminderQueueStatus status;

    LocalDateTime scheduledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    ReminderTemplate reminderTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    PhaseDetail phaseDetail;

}

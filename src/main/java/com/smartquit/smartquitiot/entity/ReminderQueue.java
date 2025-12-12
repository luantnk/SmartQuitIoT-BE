package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.ReminderQueueStatus;
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

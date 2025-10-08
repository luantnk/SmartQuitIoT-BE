package com.smartquit.smartquitiot.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    @ManyToOne(fetch = FetchType.LAZY)
    Account sender;
    @Column(columnDefinition = "TEXT")
    String content;
    @ManyToOne(fetch = FetchType.LAZY)
    Conversation conversation;
    @CreationTimestamp
    LocalDateTime sentAt;
    boolean isRead=false;
}

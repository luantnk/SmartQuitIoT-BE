package com.smartquit.smartquitiot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartquit.smartquitiot.enums.NotificationType;
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
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    String title;
    @Column(columnDefinition = "TEXT")
    String content;
    boolean isRead = false;
    boolean isDeleted = false;
    //điều hướng web
    String url;

    //điều hướng mobile
    String deepLink;   // ví dụ: smartquit://achievement/42

    // gợi ý: một số UI ico n
    String icon = "https://res.cloudinary.com/dhmmm2sq1/image/upload/v1761473137/logo_gb1xi7.png";

    @Enumerated(EnumType.STRING)
    NotificationType notificationType; // ACHIEVEMENT, SYSTEM, PHASE, QUIT_PLAN,MISSION,APPOINTMENT_BOOKED,APPOINTMENT_CANCELLED,APPOINTMENT_REMINDER

    @CreationTimestamp
    LocalDateTime createdAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    Account account;

}

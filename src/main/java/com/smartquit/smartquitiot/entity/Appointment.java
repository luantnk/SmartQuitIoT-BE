package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.AppointmentStatus;
import com.smartquit.smartquitiot.enums.CancelledBy;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    String name;

    LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    Coach coach;

    @Enumerated(EnumType.STRING)
    AppointmentStatus appointmentStatus; // PENDING, IN_PROGRESS, COMPLETED, CANCELLED

    @Enumerated(EnumType.STRING)
    CancelledBy cancelledBy; // MEMBER, COACH

    LocalDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    CoachWorkSchedule coachWorkSchedule;


}

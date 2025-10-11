package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.CoachWorkScheduleStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoachWorkSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    LocalDate date;
    @Enumerated(EnumType.STRING)
    CoachWorkScheduleStatus status;
    @ManyToOne(fetch = FetchType.LAZY)
    Slot slot;
    @ManyToOne(fetch = FetchType.LAZY)
    Coach coach;


}

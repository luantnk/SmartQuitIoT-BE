package com.smartquit.smartquitiot.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    LocalTime startTime;
    LocalTime endTime;
}

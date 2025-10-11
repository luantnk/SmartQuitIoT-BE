package com.smartquit.smartquitiot.entity;


import jakarta.persistence.*;
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

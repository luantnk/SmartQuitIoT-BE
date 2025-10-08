package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.MetricType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Metric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    String name;
    String value;
    @Enumerated(EnumType.STRING)
    MetricType type;

    @ManyToOne(fetch = FetchType.LAZY)
    Member member;
}

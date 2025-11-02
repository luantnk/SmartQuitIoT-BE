package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.DurationUnit;
import com.smartquit.smartquitiot.enums.MembershipPackageType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MembershipPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    String name;
    String description;
    long price;

    @Enumerated(EnumType.STRING)
    MembershipPackageType type; //TRIAL, STANDARD, PREMIUM

    int duration;

    @Enumerated(EnumType.STRING)
    DurationUnit durationUnit; //DAY, MONTH

    List<String> features;
}

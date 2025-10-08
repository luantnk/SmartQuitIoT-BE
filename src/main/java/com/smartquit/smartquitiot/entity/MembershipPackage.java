package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.DurationUnit;
import com.smartquit.smartquitiot.enums.MembershipPackageType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
    double price;
    @Enumerated(EnumType.STRING)
    MembershipPackageType type;
    int duration;
    @Enumerated(EnumType.STRING)
    DurationUnit durationUnit;

}

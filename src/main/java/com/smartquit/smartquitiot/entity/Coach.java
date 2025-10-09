package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.Gender;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "coach")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Coach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    String firstName;
    String lastName;
    String avatarUrl;
    String certificateUrl;
    String bio;
    @Enumerated(EnumType.STRING)
    Gender gender;
    int ratingCount = 0;
    double ratingAvg = 0.0;
    int experienceYears;
    String specializations;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id")
    Account account;


}

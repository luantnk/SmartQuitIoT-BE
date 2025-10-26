package com.smartquit.smartquitiot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartquit.smartquitiot.enums.Gender;
import com.smartquit.smartquitiot.validator.DobConstraint;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "member")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    @NotEmpty(message = "FirstName is required")
    String firstName;
    @NotEmpty(message = "LastName is required")
    String lastName;
    String avatarUrl;
    @Enumerated(EnumType.STRING)
    Gender gender;
    @DobConstraint(min = 14, message = "Member must greater than 14 years old")
    LocalDate dob;
    boolean isUsedFreeTrial = false;
    @UpdateTimestamp
    LocalDateTime modifiedAt;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    Account account;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL,mappedBy = "member")
    List<QuitPlan> quitPlans;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL,mappedBy = "member")
    List<Notification> notifications;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL,mappedBy = "member")
    List<HealthRecovery> healthRecoveries;

    @JsonIgnore
    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    Metric metric;
}

package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.validator.DobConstraint;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    @Email(message = "Email is invalid", regexp = "^[A-Za-z0-9+_.-]+@(.+)$")
    @Column(name = "email",unique = true, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    String email;
    @NotEmpty(message = "FirstName is required")
    String firstName;
    @NotEmpty(message = "LastName is required")
    String lastName;
    String avatarUrl;
    String gender;
    @DobConstraint(min = 18, message = "Member must greater than 18")
    LocalDate dob;
    int age;
    boolean isUsedFreeTrial = false;
    @UpdateTimestamp
    LocalDateTime modifiedAt;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id")
    Account account;

}

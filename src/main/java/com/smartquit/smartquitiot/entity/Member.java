package com.smartquit.smartquitiot.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
    @Email
    String email;
    String fullName;
    String avatarUrl;
    boolean isFirstLogin = true;
    boolean isUsedFreeTrial = false;
    @OneToOne(cascade = CascadeType.ALL)
    Account account;


}

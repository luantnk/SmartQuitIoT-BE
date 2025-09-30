package com.smartquit.smartquitiot.entity;

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
    @Email
    String email;
    String firstName;
    String lastName;
    String avatarUrl;
    @OneToOne(cascade = CascadeType.ALL)
    Account account;


}

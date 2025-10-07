package com.smartquit.smartquitiot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smartquit.smartquitiot.enums.AccountType;
import com.smartquit.smartquitiot.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "account")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    @Column(name = "username",unique = true, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    String username;
    @JsonIgnore
    String password;
    @Enumerated(EnumType.STRING)
    Role role;
    @Enumerated(EnumType.STRING)
    AccountType accountType;
    boolean isActive = true;
    boolean isBanned = false;
    boolean isFirstLogin = true;
    @CreationTimestamp
    LocalDateTime createdAt;


    @OneToOne(mappedBy = "account", optional = false)
    Member member;

    @OneToOne(mappedBy = "account", optional = false)
    Coach coach;

}

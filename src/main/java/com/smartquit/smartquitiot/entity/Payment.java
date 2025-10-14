package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    long orderCode;
    String paymentLinkId;
    @CreationTimestamp
    LocalDateTime createdAt;
    long amount;
    PaymentStatus status = PaymentStatus.SUCCESS;

    @OneToOne
    @JoinColumn(name = "subscription_id")
    MembershipSubscription subscription;

}

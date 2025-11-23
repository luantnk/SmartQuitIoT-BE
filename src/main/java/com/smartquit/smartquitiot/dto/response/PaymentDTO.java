package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartquit.smartquitiot.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentDTO {

    int id;
    long orderCode;
    String paymentLinkId;
    @CreationTimestamp
    LocalDateTime createdAt;
    long amount;
    PaymentStatus status;
    MemberDTO member;
    MembershipSubscriptionDTO subscription;
}

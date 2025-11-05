package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MembershipSubscriptionDTO {

    Integer id;
    LocalDate startDate;
    LocalDate endDate;
    String status;
    LocalDateTime createdAt;
    Long orderCode;
    Long totalAmount;
    MembershipPackageDTO membershipPackage;
    MemberDTO member;

}

package com.smartquit.smartquitiot.dto.request;

import com.smartquit.smartquitiot.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentProcessRequest {

    String code;
    String id;
    Boolean cancel;
    PaymentStatus status;
    Long orderCode;
}

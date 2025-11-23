package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.response.PaymentDTO;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface PaymentService {
    Map<String, Object> getPaymentStatistics();

    Page<PaymentDTO> getPayments(int page, int size, String search);
}

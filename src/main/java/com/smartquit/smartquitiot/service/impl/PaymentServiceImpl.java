package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.repository.PaymentRepository;
import com.smartquit.smartquitiot.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    public Map<String, Object> getPaymentStatistics() {
        int totalPayments = paymentRepository.findAll().size();
        long totalAmount = paymentRepository.findAll()
                .stream()
                .mapToLong(payment -> payment.getAmount())
                .sum();
        Map<String, Object> map = new HashMap<>();
        map.put("totalPayments", totalPayments);
        map.put("totalAmount", totalAmount);
        return map;
    }
}

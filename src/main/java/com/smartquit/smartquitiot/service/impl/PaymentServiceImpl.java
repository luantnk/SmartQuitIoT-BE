package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.PaymentDTO;
import com.smartquit.smartquitiot.entity.Payment;
import com.smartquit.smartquitiot.mapper.PaymentMapper;
import com.smartquit.smartquitiot.repository.PaymentRepository;
import com.smartquit.smartquitiot.service.PaymentService;
import com.smartquit.smartquitiot.specifications.PaymentSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

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

    @Override
    public Page<PaymentDTO> getPayments(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Specification<Payment> spec = Specification.allOf(PaymentSpecification.hasSearchString(search));
        Page<Payment> payments = paymentRepository.findAll(spec,pageable);

        return payments.map(paymentMapper::toDTO);
    }
}

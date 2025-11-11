package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/payment")
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/statistics")
    public ResponseEntity<?> getPaymentStatistics() {
        return ResponseEntity.ok(paymentService.getPaymentStatistics());
    }
}

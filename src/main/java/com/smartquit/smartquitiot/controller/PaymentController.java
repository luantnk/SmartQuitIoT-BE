package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.response.PaymentDTO;
import com.smartquit.smartquitiot.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/payments")
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/statistics")
    public ResponseEntity<?> getPaymentStatistics() {
        return ResponseEntity.ok(paymentService.getPaymentStatistics());
    }

    @GetMapping("/all")
    public ResponseEntity<Page<PaymentDTO>> getAllPayments(
            @RequestParam (name = "page", defaultValue = "0") int page,
            @RequestParam (name = "size", defaultValue = "10") int size,
            @RequestParam (name = "search", defaultValue = "") String search
    ) {
        return ResponseEntity.ok(paymentService.getPayments(page, size, search));
    }
}

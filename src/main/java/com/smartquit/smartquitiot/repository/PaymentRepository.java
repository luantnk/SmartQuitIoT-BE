package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
}

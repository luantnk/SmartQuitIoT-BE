package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Page<Payment> findAll(Specification<Payment> spec ,Pageable pageable);
}

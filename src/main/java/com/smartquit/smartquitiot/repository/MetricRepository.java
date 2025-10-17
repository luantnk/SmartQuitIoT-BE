package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Metric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MetricRepository extends JpaRepository<Metric , Integer> {

    Optional<Metric> findByMemberId(Integer memberId);
}

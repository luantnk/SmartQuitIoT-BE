package com.smartquit.smartquitiot.repository;


import com.smartquit.smartquitiot.entity.QuitPlan;
import com.smartquit.smartquitiot.enums.QuitPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuitPlanRepository extends JpaRepository<QuitPlan,Integer> {
    QuitPlan findByStatus(QuitPlanStatus status);
    QuitPlan findTopByOrderByCreatedAtDesc();
    Optional<QuitPlan> findByMemberId(Integer memberId);
}
package com.smartquit.smartquitiot.repository;


import com.smartquit.smartquitiot.entity.QuitPlan;
import com.smartquit.smartquitiot.enums.QuitPlanStatus;
import com.smartquit.smartquitiot.toolcalling.QuitPlanTools;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface QuitPlanRepository extends JpaRepository<QuitPlan,Integer> {
    QuitPlan findByStatus(QuitPlanStatus status);
    QuitPlan findTopByOrderByCreatedAtDesc();
    QuitPlan findByMember_IdAndStatus(int memberId, QuitPlanStatus status);
}

package com.smartquit.smartquitiot.repository;


import com.smartquit.smartquitiot.entity.QuitPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuitPlanRepository extends JpaRepository<QuitPlan,Integer> {

    Optional<QuitPlan> findByMemberId(Integer memberId);
}

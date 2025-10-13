package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Phase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhaseRepository extends JpaRepository<Phase, Integer> {
    Optional<Phase> findByQuitPlan_IdAndName(Integer quitPlanId, String name);
}

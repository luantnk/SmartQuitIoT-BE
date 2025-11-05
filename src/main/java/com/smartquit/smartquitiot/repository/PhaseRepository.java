package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Phase;
import com.smartquit.smartquitiot.enums.PhaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhaseRepository extends JpaRepository<Phase, Integer> {
    Optional<Phase> findByQuitPlan_IdAndName(Integer quitPlanId, String name);

    Optional<Phase> findByStatusAndQuitPlan_Id(PhaseStatus status,  Integer quitPlanId);
    Optional<Phase> findByIdAndQuitPlan_Id (Integer phaseId,Integer quitPlanId);
}

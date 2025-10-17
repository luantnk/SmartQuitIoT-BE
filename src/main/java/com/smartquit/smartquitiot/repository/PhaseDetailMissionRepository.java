package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.PhaseDetailMission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhaseDetailMissionRepository extends JpaRepository<PhaseDetailMission, Integer> {
    Optional<PhaseDetailMission> findByPhaseDetail_IdAndMission_Code(Integer phaseDetailId, String code);
}

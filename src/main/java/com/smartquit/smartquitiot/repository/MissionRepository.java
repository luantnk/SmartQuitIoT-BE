package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Mission;
import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.enums.MissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Integer> {
    List<Mission> findByPhaseAndStatus(MissionPhase phase, MissionStatus status);
    Optional<Mission> findByCode(String code);
}


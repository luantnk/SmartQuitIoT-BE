package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Mission;
import com.smartquit.smartquitiot.enums.MissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Integer> {
    List<Mission> findAllByStatus(MissionStatus status);
}

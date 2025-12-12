package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Mission;
import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.enums.MissionStatus;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Integer> {
    List<Mission> findByPhaseAndStatus(MissionPhase phase, MissionStatus status);
    Optional<Mission> findByCode(String code);


    Page<Mission> findAll(Specification<Mission> spec, Pageable pageable);

    boolean existsByCode(@NotBlank(message = "Code is required") String code);
}


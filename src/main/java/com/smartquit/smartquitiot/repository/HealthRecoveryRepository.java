package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.HealthRecovery;
import com.smartquit.smartquitiot.enums.HealthRecoveryDataName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HealthRecoveryRepository extends JpaRepository<HealthRecovery, Integer> {
    Optional<HealthRecovery> findByNameAndMemberId(HealthRecoveryDataName name, int memberId);
    List<HealthRecovery> findByMemberId(Integer memberId);
}

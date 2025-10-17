package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.PhaseDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PhaseDetailRepository extends JpaRepository<PhaseDetail, Integer> {
    @Modifying
    @Query("DELETE FROM PhaseDetail d WHERE d.phase.id = :phaseId")
    void deleteByPhaseId(Integer phaseId);

    List<PhaseDetail> findByPhase_IdOrderByDateAsc(Integer phaseId);
}

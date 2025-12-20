package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.dto.response.PhaseDetailMissionResponseDTO;
import com.smartquit.smartquitiot.entity.PhaseDetailMission;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PhaseDetailMissionRepository extends JpaRepository<PhaseDetailMission, Integer> {
    Optional<PhaseDetailMission> findByPhaseDetail_IdAndMission_Code(Integer phaseDetailId, String code);


    @Query("""
        select m
        from PhaseDetailMission m
        join fetch m.phaseDetail d
        where d.phase.id = :phaseId
        order by d.date asc, m.id asc
    """)
    List<PhaseDetailMission> findAllMissionsInPhase(@Param("phaseId") Integer phaseId);

}

package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.CoachWorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface CoachWorkScheduleRepository extends JpaRepository<CoachWorkSchedule, Integer> {
    boolean existsByCoachIdAndDateAndSlotId(int coachId, LocalDate date, int slotId);
}

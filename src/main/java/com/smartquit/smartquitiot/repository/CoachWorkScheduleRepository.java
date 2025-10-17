package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.CoachWorkSchedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CoachWorkScheduleRepository extends JpaRepository<CoachWorkSchedule, Integer> {
    boolean existsByCoachIdAndDateAndSlotId(int coachId, LocalDate date, int slotId);
    @EntityGraph(attributePaths = {"coach"})
    @Query("SELECT DISTINCT cws FROM CoachWorkSchedule cws " +
            "JOIN FETCH cws.coach coach " +
            "WHERE cws.date BETWEEN :start AND :end")
    List<CoachWorkSchedule> findAllByDateBetweenWithCoach(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("SELECT DISTINCT cws.coach.id FROM CoachWorkSchedule cws " +
            "WHERE cws.date = :date")
    List<Integer> findAllCoachIdsByDate(@Param("date") LocalDate date);

    @Query("SELECT cws FROM CoachWorkSchedule cws " +
            "WHERE cws.coach.id = :coachId AND cws.date = :date")
    List<CoachWorkSchedule> findAllByCoachAndDate(@Param("coachId") int coachId,
                                                  @Param("date") LocalDate date);

}

package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.CoachWorkSchedule;
import com.smartquit.smartquitiot.enums.CoachWorkScheduleStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    // Lấy danh sách slot theo ngày của coach
    @Query("SELECT cws FROM CoachWorkSchedule cws " +
            "JOIN FETCH cws.slot s " +
            "WHERE cws.coach.id = :coachId AND cws.date = :date AND cws.status = :status")
    List<CoachWorkSchedule> findAllByCoachIdAndDateAndStatusWithSlot(
            @Param("coachId") int coachId,
            @Param("date") LocalDate date,
            @Param("status") CoachWorkScheduleStatus status
    );
    // Kiểm tra xem slot này có tồn tại không
    @Query("SELECT cws FROM CoachWorkSchedule cws " +
            "WHERE cws.coach.id = :coachId AND cws.date = :date AND cws.slot.id = :slotId")
    Optional<CoachWorkSchedule> findByCoachIdAndDateAndSlotId(
            @Param("coachId") int coachId,
            @Param("date") LocalDate date,
            @Param("slotId") int slotId
    );
    // Dự tính cho cron - update batch theo ngày
    @Query("SELECT cws FROM CoachWorkSchedule cws " +
            "JOIN FETCH cws.slot s " +
            "WHERE cws.date = :date AND cws.status IN :statuses")
    List<CoachWorkSchedule> findAllByDateAndStatusIn(
            @Param("date") LocalDate date,
            @Param("statuses") List<CoachWorkScheduleStatus> statuses
    );

}

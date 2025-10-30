package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Appointment;
import com.smartquit.smartquitiot.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    // kiểm tra appointment active via CoachWorkSchedule (coach + date + slot) ----------
    @Query("SELECT (COUNT(a) > 0) FROM Appointment a " +
            "WHERE a.coachWorkSchedule.coach.id = :coachId " +
            "  AND a.coachWorkSchedule.slot.id = :slotId " +
            "  AND a.coachWorkSchedule.date = :date " +
            "  AND a.appointmentStatus <> com.smartquit.smartquitiot.enums.AppointmentStatus.CANCELLED")
    boolean existsActiveByCoachSlotDate(@Param("coachId") int coachId,
                                        @Param("slotId") int slotId,
                                        @Param("date") LocalDate date);

    @Query("SELECT a FROM Appointment a " +
            "JOIN FETCH a.coachWorkSchedule cws " +
            "JOIN FETCH cws.slot s " +
            "WHERE a.date = :date AND a.appointmentStatus IN :statuses")
    List<Appointment> findAllByDateAndStatusIn(@Param("date") LocalDate date,
                                               @Param("statuses") List<AppointmentStatus> statuses);

    List<Appointment> findAllByMemberId(int memberId);


    List<Appointment> findAllByCoachId(int coachId);

    @Query("SELECT DISTINCT a FROM Appointment a " +
            "JOIN FETCH a.coach c " +
            "LEFT JOIN FETCH a.coachWorkSchedule cws " +
            "LEFT JOIN FETCH cws.slot s " +
            "LEFT JOIN FETCH a.member m " +
            "WHERE c.account.id = :accountId")
    List<Appointment> findAllByCoachAccountId(@Param("accountId") int accountId);
}

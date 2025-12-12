package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Appointment;
import com.smartquit.smartquitiot.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
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

    @Query("SELECT COUNT(a) FROM Appointment a " +
            "WHERE a.member.id = :memberId " +
            "  AND a.date BETWEEN :start AND :end " +
            "  AND ( a.appointmentStatus <> com.smartquit.smartquitiot.enums.AppointmentStatus.CANCELLED " +
            "        OR a.cancelledBy = com.smartquit.smartquitiot.enums.CancelledBy.MEMBER )")
    long countActiveByMemberIdAndDateBetween(@Param("memberId") int memberId,
                                             @Param("start") LocalDate start,
                                             @Param("end") LocalDate end);

    @Query("""
    SELECT a FROM Appointment a
    JOIN a.coachWorkSchedule cws
    JOIN cws.slot s
    WHERE a.appointmentStatus = :status
      AND a.date = :date
      AND s.startTime BETWEEN :from AND :to
""")
    List<Appointment> findAppointmentsForReminder(
            @Param("date") LocalDate date,
            @Param("from") LocalTime from,
            @Param("to") LocalTime to,
            @Param("status") AppointmentStatus status
    );

    // Count active appointments from a specific date onwards (for slot reseed validation)
    @Query("SELECT COUNT(a) FROM Appointment a " +
           "WHERE a.date >= :fromDate " +
           "  AND a.appointmentStatus <> com.smartquit.smartquitiot.enums.AppointmentStatus.CANCELLED" +
            "  AND a.appointmentStatus <> com.smartquit.smartquitiot.enums.AppointmentStatus.COMPLETED")
    long  countActiveAppointmentsFromDate(@Param("fromDate") LocalDate fromDate);

    // Find appointments by CoachWorkSchedule IDs (for cleanup during slot reseed)
    @Query("SELECT a FROM Appointment a " +
           "WHERE a.coachWorkSchedule.id IN :cwsIds")
    List<Appointment> findByCoachWorkScheduleIds(@Param("cwsIds") List<Integer> cwsIds);
    Page<Appointment> findAll(Specification<Appointment> specification, Pageable pageable);

    boolean existsByCoachIdAndAppointmentStatusOrAppointmentStatus(int coachId, AppointmentStatus fistStatus, AppointmentStatus secondStatus);
    boolean existsByMemberIdAndAppointmentStatusOrAppointmentStatus(int memberId, AppointmentStatus fistStatus, AppointmentStatus secondStatus);

    @Query("SELECT COUNT(a) FROM Appointment a " +
       "WHERE a.date = :date " +
       "AND a.appointmentStatus <> com.smartquit.smartquitiot.enums.AppointmentStatus.CANCELLED")
    long countByDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(a) FROM Appointment a " +
       "WHERE a.appointmentStatus = com.smartquit.smartquitiot.enums.AppointmentStatus.PENDING")
    long countPendingRequests();

    @Query("SELECT COUNT(a) FROM Appointment a " +
       "WHERE a.appointmentStatus = com.smartquit.smartquitiot.enums.AppointmentStatus.COMPLETED " +
       "AND a.date BETWEEN :startDate AND :endDate")
    long countCompletedBetween(@Param("startDate") LocalDate startDate, 
                          @Param("endDate") LocalDate endDate);

    @Query("SELECT a FROM Appointment a " +
       "JOIN FETCH a.member m " +
       "JOIN FETCH a.coachWorkSchedule cws " +
       "JOIN FETCH cws.slot s " +
       "WHERE a.date = :date " +
       "AND a.appointmentStatus <> com.smartquit.smartquitiot.enums.AppointmentStatus.CANCELLED " +
       "ORDER BY s.startTime DESC")
    List<Appointment> findUpcomingByDate(@Param("date") LocalDate date);

    /**
     * Tìm appointments trùng thời gian: cùng member, cùng date, cùng slot nhưng khác coach.
     * Dùng để cảnh báo khi member đặt lịch trùng thời gian với coach khác.
     */
    @Query("SELECT a FROM Appointment a " +
            "JOIN FETCH a.coachWorkSchedule cws " +
            "JOIN FETCH cws.slot s " +
            "JOIN FETCH a.coach c " +
            "WHERE a.member.id = :memberId " +
            "  AND a.date = :date " +
            "  AND cws.slot.id = :slotId " +
            "  AND c.id <> :excludeCoachId " +
            "  AND a.appointmentStatus <> com.smartquit.smartquitiot.enums.AppointmentStatus.CANCELLED")
    List<Appointment> findOverlappingAppointments(@Param("memberId") int memberId,
                                                   @Param("date") LocalDate date,
                                                   @Param("slotId") int slotId,
                                                   @Param("excludeCoachId") int excludeCoachId);
}

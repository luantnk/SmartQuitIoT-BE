package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    // Kiểm tra xem slot đã bị dặt trong ngày đó chưa
    boolean existsByCoachIdAndSlotIdAndDate(int coachId, int slotId, LocalDate date);

    // Tìm appointment (khi cần hủy hay gì đó)
    @Query("SELECT a FROM Appointment a " +
            "WHERE a.coach.id = :coachId AND a.slot.id = :slotId AND a.date = :date")
    Appointment findByCoachSlotAndDate(@Param("coachId") int coachId,
                                       @Param("slotId") int slotId,
                                       @Param("date") LocalDate date);
    List<Appointment> findAllByMemberId(int memberId);

}
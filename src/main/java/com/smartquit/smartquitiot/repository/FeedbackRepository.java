package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    boolean existsByAppointment_Id(int appointmentId);
    // trả list appointmentId đã có feedback của member
    @Query("SELECT f.appointment.id FROM Feedback f " +
            "WHERE f.appointment.id IN :appointmentIds " +
            "  AND f.member.account.id = :memberAccountId")
    List<Integer> findRatedAppointmentIdsByAppointmentIdsAndMemberAccountId(
            @Param("appointmentIds") List<Integer> appointmentIds,
            @Param("memberAccountId") int memberAccountId);
}


package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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


    @EntityGraph(attributePaths = {
            "appointment",
            "appointment.coachWorkSchedule",
            "appointment.coachWorkSchedule.slot",
            "member"
    })
    Page<Feedback> findAllByCoach_Id(int coachId, Pageable pageable);

    // Find feedbacks by appointment IDs (for cleanup during slot reseed)
    @Query("SELECT f FROM Feedback f " +
           "WHERE f.appointment.id IN :appointmentIds")
    List<Feedback> findByAppointmentIds(@Param("appointmentIds") List<Integer> appointmentIds);

}


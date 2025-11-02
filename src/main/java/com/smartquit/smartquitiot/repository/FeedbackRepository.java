package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    boolean existsByAppointment_Id(int appointmentId);
}

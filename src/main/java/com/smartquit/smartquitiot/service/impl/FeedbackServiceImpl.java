
package com.smartquit.smartquitiot.service.impl;
import com.smartquit.smartquitiot.dto.request.FeedbackRequest;
import com.smartquit.smartquitiot.dto.response.FeedbackResponse;
import com.smartquit.smartquitiot.entity.Appointment;
import com.smartquit.smartquitiot.entity.Coach;
import com.smartquit.smartquitiot.entity.Feedback;
import com.smartquit.smartquitiot.mapper.FeedbackMapper;
import com.smartquit.smartquitiot.repository.AppointmentRepository;
import com.smartquit.smartquitiot.repository.CoachRepository;
import com.smartquit.smartquitiot.repository.FeedbackRepository;
import com.smartquit.smartquitiot.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final AppointmentRepository appointmentRepository;
    private final CoachRepository coachRepository;

    @Override
    @Transactional
    public void createFeedback(int appointmentId, int memberAccountId, FeedbackRequest request) {
        if (feedbackRepository.existsByAppointment_Id(appointmentId)) {
            throw new IllegalStateException("Feedback already exists for this appointment");
        }

        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        if (appt.getMember() == null || appt.getMember().getAccount() == null) {
            throw new IllegalStateException("Appointment has no member/account");
        }

        final int ownerAccountId = appt.getMember().getAccount().getId();
        if (ownerAccountId != memberAccountId) {
            throw new SecurityException("Member not authorized to give feedback for this appointment");
        }

        // Only allow feedback after appointment is COMPLETED
        if (appt.getAppointmentStatus() == null || !appt.getAppointmentStatus().name().equalsIgnoreCase("COMPLETED")) {
            throw new IllegalStateException("Can only submit feedback for completed appointments");
        }

        // 3. create feedback
        Feedback fb = new Feedback();
        fb.setStar(request.getStar());
        fb.setContent(request.getContent());
        fb.setCreatedAt(LocalDateTime.now());
        fb.setAppointment(appt);
        fb.setMember(appt.getMember());
        fb.setCoach(appt.getCoach());

        feedbackRepository.save(fb);

        // 4. update coach aggregates with lock
        Coach coach = coachRepository.findByIdForUpdate(appt.getCoach().getId())
                .orElseThrow(() -> new IllegalArgumentException("Coach not found"));

        final int oldCount = coach.getRatingCount();
        final double oldAvg = coach.getRatingAvg();

        final int newCount = oldCount + 1;
        final double newAvg = ((oldAvg * oldCount) + request.getStar()) / (double) newCount;

        coach.setRatingCount(newCount);
        coach.setRatingAvg(newAvg);

        coachRepository.save(coach);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getFeedbacksByCoachId(int coachId, Pageable pageable) {
        Page<Feedback> page = feedbackRepository.findAllByCoach_Id(coachId, pageable);
        return page.map(FeedbackMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getFeedbacksForCoachAccount(int accountId, Pageable pageable) {
        Coach coach = coachRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Coach not found for accountId: " + accountId));
        return getFeedbacksByCoachId(coach.getId(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponse getFeedbackByAppointmentIdForMember(int appointmentId, int memberAccountId) {
        // Kiểm tra appointment tồn tại
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        // Kiểm tra member có quyền (phải là owner của appointment)
        if (appointment.getMember() == null || appointment.getMember().getAccount() == null) {
            throw new IllegalStateException("Appointment has no member/account");
        }

        final int ownerAccountId = appointment.getMember().getAccount().getId();
        if (ownerAccountId != memberAccountId) {
            throw new SecurityException("Member not authorized to view feedback for this appointment");
        }

        Feedback feedback = feedbackRepository.findByAppointmentIdAndMemberAccountId(appointmentId, memberAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback not found for this appointment"));

        return FeedbackMapper.toResponse(feedback);
    }

}

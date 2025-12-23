package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.FeedbackRequest;
import com.smartquit.smartquitiot.dto.response.FeedbackResponse;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Appointment;
import com.smartquit.smartquitiot.entity.Coach;
import com.smartquit.smartquitiot.entity.Feedback;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.enums.AppointmentStatus;
import com.smartquit.smartquitiot.mapper.FeedbackMapper;
import com.smartquit.smartquitiot.repository.AppointmentRepository;
import com.smartquit.smartquitiot.repository.CoachRepository;
import com.smartquit.smartquitiot.repository.FeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CoachRepository coachRepository;

    @InjectMocks
    private FeedbackServiceImpl feedbackService;

    // Test data
    private Account memberAccount;
    private Member member;
    private Account coachAccount;
    private Coach coach;
    private Appointment appointment;
    private FeedbackRequest feedbackRequest;
    private Feedback feedback;

    @BeforeEach
    void setUp() {
        // Setup member account
        memberAccount = new Account();
        memberAccount.setId(100);
        memberAccount.setUsername("memberuser");
        memberAccount.setEmail("member@example.com");

        // Setup member
        member = new Member();
        member.setId(1);
        member.setFirstName("John");
        member.setLastName("Doe");
        member.setAccount(memberAccount);
        memberAccount.setMember(member);

        // Setup coach account
        coachAccount = new Account();
        coachAccount.setId(200);
        coachAccount.setUsername("coachuser");
        coachAccount.setEmail("coach@example.com");

        // Setup coach
        coach = new Coach();
        coach.setId(10);
        coach.setFirstName("Jane");
        coach.setLastName("Smith");
        coach.setRatingCount(5);
        coach.setRatingAvg(4.2);
        coach.setAccount(coachAccount);
        coachAccount.setCoach(coach);

        // Setup appointment
        appointment = new Appointment();
        appointment.setId(50);
        appointment.setAppointmentStatus(AppointmentStatus.COMPLETED);
        appointment.setMember(member);
        appointment.setCoach(coach);
        appointment.setDate(java.time.LocalDate.now());

        // Setup feedback request
        feedbackRequest = new FeedbackRequest();
        feedbackRequest.setStar(5);
        feedbackRequest.setContent("Great service!");

        // Setup feedback
        feedback = new Feedback();
        feedback.setId(1);
        feedback.setStar(5);
        feedback.setContent("Great service!");
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setAppointment(appointment);
        feedback.setMember(member);
        feedback.setCoach(coach);
    }

    // ========== createFeedback Tests ==========

    @Test
    void should_create_feedback_successfully_for_completed_appointment() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int memberAccountId = 100;

        when(feedbackRepository.existsByAppointment_Id(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(coachRepository.findByIdForUpdate(coach.getId())).thenReturn(Optional.of(coach));
        when(coachRepository.save(any(Coach.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ===== WHEN =====
        feedbackService.createFeedback(appointmentId, memberAccountId, feedbackRequest);

        // ===== THEN =====
        // Verify coach rating was updated correctly
        assertThat(coach.getRatingCount()).isEqualTo(6); // 5 + 1
        // Expected average: (4.2 * 5 + 5) / 6 = (21 + 5) / 6 = 26 / 6 = 4.333...
        assertThat(coach.getRatingAvg()).isEqualTo(26.0 / 6.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void should_calculate_coach_rating_correctly_for_first_feedback() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int memberAccountId = 100;

        // Coach with no previous ratings
        Coach newCoach = new Coach();
        newCoach.setId(11);
        newCoach.setRatingCount(0);
        newCoach.setRatingAvg(0.0);
        appointment.setCoach(newCoach);

        when(feedbackRepository.existsByAppointment_Id(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(coachRepository.findByIdForUpdate(newCoach.getId())).thenReturn(Optional.of(newCoach));
        when(coachRepository.save(any(Coach.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ===== WHEN =====
        feedbackService.createFeedback(appointmentId, memberAccountId, feedbackRequest);

        // ===== THEN =====
        assertThat(newCoach.getRatingCount()).isEqualTo(1);
        assertThat(newCoach.getRatingAvg()).isEqualTo(5.0); // First rating
    }

    @Test
    void should_throw_exception_when_feedback_already_exists() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int memberAccountId = 100;

        when(feedbackRepository.existsByAppointment_Id(appointmentId)).thenReturn(true);

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> feedbackService.createFeedback(appointmentId, memberAccountId, feedbackRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Feedback already exists");

        verify(feedbackRepository, never()).save(any());
        verify(coachRepository, never()).findByIdForUpdate(anyInt());
    }

    @Test
    void should_throw_exception_when_appointment_not_found() {
        // ===== GIVEN =====
        int appointmentId = 999;
        int memberAccountId = 100;

        when(feedbackRepository.existsByAppointment_Id(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> feedbackService.createFeedback(appointmentId, memberAccountId, feedbackRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Appointment not found");

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void should_throw_exception_when_appointment_has_no_member() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int memberAccountId = 100;

        appointment.setMember(null);

        when(feedbackRepository.existsByAppointment_Id(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> feedbackService.createFeedback(appointmentId, memberAccountId, feedbackRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Appointment has no member/account");

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void should_throw_exception_when_member_not_authorized() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int memberAccountId = 999; // Different account ID

        when(feedbackRepository.existsByAppointment_Id(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> feedbackService.createFeedback(appointmentId, memberAccountId, feedbackRequest))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("not authorized");

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void should_throw_exception_when_appointment_not_completed() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int memberAccountId = 100;

        appointment.setAppointmentStatus(AppointmentStatus.PENDING);

        when(feedbackRepository.existsByAppointment_Id(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> feedbackService.createFeedback(appointmentId, memberAccountId, feedbackRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only submit feedback for completed appointments");

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void should_throw_exception_when_appointment_status_is_null() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int memberAccountId = 100;

        appointment.setAppointmentStatus(null);

        when(feedbackRepository.existsByAppointment_Id(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> feedbackService.createFeedback(appointmentId, memberAccountId, feedbackRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only submit feedback for completed appointments");

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void should_throw_exception_when_coach_not_found() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int memberAccountId = 100;

        when(feedbackRepository.existsByAppointment_Id(appointmentId)).thenReturn(false);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(coachRepository.findByIdForUpdate(coach.getId())).thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> feedbackService.createFeedback(appointmentId, memberAccountId, feedbackRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coach not found");
    }

    // ========== getFeedbacksByCoachId Tests ==========

    @Test
    void should_get_feedbacks_by_coach_id_successfully() {
        // ===== GIVEN =====
        int coachId = 10;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Feedback> feedbackPage = new PageImpl<>(List.of(feedback), pageable, 1);

        FeedbackResponse expectedResponse = FeedbackResponse.builder()
                .id(1)
                .rating(5)
                .content("Great service!")
                .build();

        when(feedbackRepository.findAllByCoach_Id(coachId, pageable)).thenReturn(feedbackPage);

        // ===== WHEN =====
        Page<FeedbackResponse> result = feedbackService.getFeedbacksByCoachId(coachId, pageable);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().size()).isEqualTo(1);
    }

    @Test
    void should_return_empty_page_when_no_feedbacks() {
        // ===== GIVEN =====
        int coachId = 10;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Feedback> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(feedbackRepository.findAllByCoach_Id(coachId, pageable)).thenReturn(emptyPage);

        // ===== WHEN =====
        Page<FeedbackResponse> result = feedbackService.getFeedbacksByCoachId(coachId, pageable);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent().isEmpty()).isTrue();
    }

    // ========== getFeedbacksForCoachAccount Tests ==========

    @Test
    void should_get_feedbacks_for_coach_account_successfully() {
        // ===== GIVEN =====
        int accountId = 200;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Feedback> feedbackPage = new PageImpl<>(List.of(feedback), pageable, 1);

        when(coachRepository.findByAccountId(accountId)).thenReturn(Optional.of(coach));
        when(feedbackRepository.findAllByCoach_Id(coach.getId(), pageable)).thenReturn(feedbackPage);

        // ===== WHEN =====
        Page<FeedbackResponse> result = feedbackService.getFeedbacksForCoachAccount(accountId, pageable);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void should_throw_exception_when_coach_not_found_for_account() {
        // ===== GIVEN =====
        int accountId = 999;
        Pageable pageable = PageRequest.of(0, 10);

        when(coachRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> feedbackService.getFeedbacksForCoachAccount(accountId, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coach not found for accountId");
    }

    // ========== getFeedbackByAppointmentIdForMember Tests ==========

    @Test
    void should_get_feedback_by_appointment_id_for_member_successfully() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int memberAccountId = 100;

        FeedbackResponse expectedResponse = FeedbackResponse.builder()
                .id(1)
                .rating(5)
                .content("Great service!")
                .build();

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(feedbackRepository.findByAppointmentIdAndMemberAccountId(appointmentId, memberAccountId))
                .thenReturn(Optional.of(feedback));

        // ===== WHEN =====
        FeedbackResponse result = feedbackService.getFeedbackByAppointmentIdForMember(appointmentId, memberAccountId);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(feedback.getId());
        assertThat(result.getRating()).isEqualTo(feedback.getStar());
        assertThat(result.getContent()).isEqualTo(feedback.getContent());
    }

    @Test
    void should_throw_exception_when_appointment_not_found_for_get_feedback() {
        // ===== GIVEN =====
        int appointmentId = 999;
        int memberAccountId = 100;

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> feedbackService.getFeedbackByAppointmentIdForMember(appointmentId, memberAccountId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Appointment not found");
    }

    @Test
    void should_throw_exception_when_member_not_authorized_to_view_feedback() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int memberAccountId = 999; // Different account ID

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> feedbackService.getFeedbackByAppointmentIdForMember(appointmentId, memberAccountId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("not authorized to view feedback");

        verify(feedbackRepository, never()).findByAppointmentIdAndMemberAccountId(anyInt(), anyInt());
    }

    @Test
    void should_throw_exception_when_feedback_not_found() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int memberAccountId = 100;

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(feedbackRepository.findByAppointmentIdAndMemberAccountId(appointmentId, memberAccountId))
                .thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> feedbackService.getFeedbackByAppointmentIdForMember(appointmentId, memberAccountId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Feedback not found");
    }
}
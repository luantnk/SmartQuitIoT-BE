package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.AppointmentRequest;
import com.smartquit.smartquitiot.dto.response.AppointmentResponse;
import com.smartquit.smartquitiot.dto.response.JoinTokenResponse;
import com.smartquit.smartquitiot.dto.response.RemainingBookingResponse;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.AppointmentStatus;
import com.smartquit.smartquitiot.enums.CancelledBy;
import com.smartquit.smartquitiot.enums.CoachWorkScheduleStatus;
import com.smartquit.smartquitiot.mapper.AppointmentMapper;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.AgoraService;
import com.smartquit.smartquitiot.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    // ===== MOCK DEPENDENCIES =====
    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CoachWorkScheduleRepository coachWorkScheduleRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private AgoraService agoraService;

    @Mock
    private MembershipSubscriptionRepository membershipSubscriptionRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    // ===== CLASS UNDER TEST =====
    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    // ===== TEST DATA =====
    private Member member;
    private Coach coach;
    private Slot slot;
    private CoachWorkSchedule cws;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setId(1);
        member.setFirstName("John");
        member.setLastName("Doe");
        Account memberAccount = new Account();
        memberAccount.setId(100);
        member.setAccount(memberAccount);

        coach = new Coach();
        coach.setId(10);
        coach.setFirstName("Jane");
        coach.setLastName("Smith");
        Account coachAccount = new Account();
        coachAccount.setId(200);
        coach.setAccount(coachAccount);

        slot = new Slot();
        slot.setId(5);
        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(10, 30));

        cws = new CoachWorkSchedule();
        cws.setCoach(coach);
        cws.setSlot(slot);
        cws.setDate(LocalDate.now().plusDays(1));
        cws.setStatus(CoachWorkScheduleStatus.AVAILABLE);

        appointment = new Appointment();
        appointment.setId(50);
        appointment.setAppointmentStatus(AppointmentStatus.PENDING);
        appointment.setMember(member);
        appointment.setCoach(coach);
        appointment.setDate(cws.getDate());
        appointment.setCoachWorkSchedule(cws);
        appointment.setCreatedAt(LocalDateTime.now());
    }

    // ========== bookAppointment Tests ==========

    @Test
    void should_book_appointment_successfully_when_all_conditions_valid() {
        // ===== GIVEN =====
        int accountId = 100;

        AppointmentRequest request = new AppointmentRequest();
        request.setCoachId(coach.getId());
        request.setSlotId(slot.getId());
        request.setDate(cws.getDate());

        when(memberRepository.findByAccountId(accountId))
                .thenReturn(Optional.of(member));

        when(membershipSubscriptionRepository.findActiveByMemberId(anyInt(), any()))
                .thenReturn(Optional.of(mockSubscription()));

        when(appointmentRepository.countActiveByMemberIdAndDateBetween(anyInt(), any(), any()))
                .thenReturn(0L);

        when(appointmentRepository.findOverlappingAppointments(anyInt(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        when(appointmentRepository.existsActiveByCoachSlotDate(anyInt(), anyInt(), any()))
                .thenReturn(false);

        when(coachWorkScheduleRepository.findByCoachIdAndDateAndSlotIdForUpdate(
                coach.getId(), cws.getDate(), slot.getId()))
                .thenReturn(Optional.of(cws));

        AppointmentResponse response = new AppointmentResponse();
        when(appointmentMapper.toResponse(any()))
                .thenReturn(response);

        when(notificationRepository.existsByAccount_IdAndNotificationTypeAndDeepLinkAndIsDeletedFalse(
                anyInt(), any(), anyString()))
                .thenReturn(false);

        // ===== WHEN =====
        AppointmentResponse result = appointmentService.bookAppointment(accountId, request);

        // ===== THEN =====
        assertThat(result).isNotNull();
        verify(appointmentRepository).save(any(Appointment.class));
        verify(coachWorkScheduleRepository).save(cws);
        verify(notificationService, atMostOnce())
                .saveAndPublish(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void should_throw_exception_when_member_not_found() {
        // ===== GIVEN =====
        int accountId = 999;
        AppointmentRequest request = new AppointmentRequest();

        when(memberRepository.findByAccountId(accountId))
                .thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> appointmentService.bookAppointment(accountId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Member not found");
    }

    @Test
    void should_throw_exception_when_no_active_subscription() {
        // ===== GIVEN =====
        int accountId = 100;
        AppointmentRequest request = new AppointmentRequest();

        when(memberRepository.findByAccountId(accountId))
                .thenReturn(Optional.of(member));

        when(membershipSubscriptionRepository.findActiveByMemberId(anyInt(), any()))
                .thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> appointmentService.bookAppointment(accountId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No remaining booking available");
    }

    @Test
    void should_throw_exception_when_no_remaining_bookings() {
        // ===== GIVEN =====
        int accountId = 100;
        AppointmentRequest request = new AppointmentRequest();

        when(memberRepository.findByAccountId(accountId))
                .thenReturn(Optional.of(member));

        MembershipSubscription sub = mockSubscription();
        when(membershipSubscriptionRepository.findActiveByMemberId(anyInt(), any()))
                .thenReturn(Optional.of(sub));

        when(appointmentRepository.countActiveByMemberIdAndDateBetween(anyInt(), any(), any()))
                .thenReturn(10L); // Đã dùng hết lượt

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> appointmentService.bookAppointment(accountId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No remaining booking available");
    }

    @Test
    void should_throw_exception_when_overlapping_appointments_exist() {
        // ===== GIVEN =====
        int accountId = 100;
        AppointmentRequest request = new AppointmentRequest();
        request.setCoachId(coach.getId());
        request.setSlotId(slot.getId());
        request.setDate(cws.getDate());

        Appointment conflictingAppointment = new Appointment();
        conflictingAppointment.setCoach(coach);
        conflictingAppointment.setCoachWorkSchedule(cws);

        when(memberRepository.findByAccountId(accountId))
                .thenReturn(Optional.of(member));

        when(membershipSubscriptionRepository.findActiveByMemberId(anyInt(), any()))
                .thenReturn(Optional.of(mockSubscription()));

        when(appointmentRepository.countActiveByMemberIdAndDateBetween(anyInt(), any(), any()))
                .thenReturn(0L);

        when(appointmentRepository.findOverlappingAppointments(anyInt(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(conflictingAppointment));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> appointmentService.bookAppointment(accountId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already have an appointment scheduled");
    }

    @Test
    void should_throw_exception_when_slot_already_booked() {
        // ===== GIVEN =====
        int accountId = 100;
        AppointmentRequest request = new AppointmentRequest();
        request.setCoachId(coach.getId());
        request.setSlotId(slot.getId());
        request.setDate(cws.getDate());

        when(memberRepository.findByAccountId(accountId))
                .thenReturn(Optional.of(member));

        when(membershipSubscriptionRepository.findActiveByMemberId(anyInt(), any()))
                .thenReturn(Optional.of(mockSubscription()));

        when(appointmentRepository.countActiveByMemberIdAndDateBetween(anyInt(), any(), any()))
                .thenReturn(0L);

        when(appointmentRepository.findOverlappingAppointments(anyInt(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        when(appointmentRepository.existsActiveByCoachSlotDate(anyInt(), anyInt(), any()))
                .thenReturn(true); // Slot đã được book

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> appointmentService.bookAppointment(accountId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("This slot has already been booked");
    }

    @Test
    void should_throw_exception_when_coach_does_not_have_slot_in_schedule() {
        // ===== GIVEN =====
        int accountId = 100;
        AppointmentRequest request = new AppointmentRequest();
        request.setCoachId(coach.getId());
        request.setSlotId(slot.getId());
        request.setDate(cws.getDate());

        when(memberRepository.findByAccountId(accountId))
                .thenReturn(Optional.of(member));

        when(membershipSubscriptionRepository.findActiveByMemberId(anyInt(), any()))
                .thenReturn(Optional.of(mockSubscription()));

        when(appointmentRepository.countActiveByMemberIdAndDateBetween(anyInt(), any(), any()))
                .thenReturn(0L);

        when(appointmentRepository.findOverlappingAppointments(anyInt(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        when(appointmentRepository.existsActiveByCoachSlotDate(anyInt(), anyInt(), any()))
                .thenReturn(false);

        when(coachWorkScheduleRepository.findByCoachIdAndDateAndSlotIdForUpdate(
                coach.getId(), cws.getDate(), slot.getId()))
                .thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> appointmentService.bookAppointment(accountId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coach does not have this slot in schedule");
    }

    @Test
    void should_throw_exception_when_slot_not_available() {
        // ===== GIVEN =====
        int accountId = 100;
        AppointmentRequest request = new AppointmentRequest();
        request.setCoachId(coach.getId());
        request.setSlotId(slot.getId());
        request.setDate(cws.getDate());

        cws.setStatus(CoachWorkScheduleStatus.BOOKED); // Slot đã bị book

        when(memberRepository.findByAccountId(accountId))
                .thenReturn(Optional.of(member));

        when(membershipSubscriptionRepository.findActiveByMemberId(anyInt(), any()))
                .thenReturn(Optional.of(mockSubscription()));

        when(appointmentRepository.countActiveByMemberIdAndDateBetween(anyInt(), any(), any()))
                .thenReturn(0L);

        when(appointmentRepository.findOverlappingAppointments(anyInt(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        when(appointmentRepository.existsActiveByCoachSlotDate(anyInt(), anyInt(), any()))
                .thenReturn(false);

        when(coachWorkScheduleRepository.findByCoachIdAndDateAndSlotIdForUpdate(
                coach.getId(), cws.getDate(), slot.getId()))
                .thenReturn(Optional.of(cws));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> appointmentService.bookAppointment(accountId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("This slot is not available");
    }

    // ========== cancelAppointment Tests ==========

    @Test
    void should_cancel_appointment_successfully_when_member_cancels() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int accountId = 100;

        LocalDate appointmentDate = LocalDate.now().plusDays(1);
        LocalTime startTime = LocalTime.of(10, 0);
        LocalDateTime appointmentDateTime = LocalDateTime.of(appointmentDate, startTime);
        ZonedDateTime appointmentZoned = appointmentDateTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        ZonedDateTime now = appointmentZoned.minusMinutes(10); // Cancel 10 phút trước (hợp lệ, cần > 5 phút)

        appointment.setDate(appointmentDate);
        appointment.setAppointmentStatus(AppointmentStatus.PENDING);
        cws.setDate(appointmentDate);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment));

        when(memberRepository.findByAccountId(accountId))
                .thenReturn(Optional.of(member));

        when(coachWorkScheduleRepository.findByCoachIdAndDateAndSlotIdForUpdate(
                anyInt(), any(), anyInt()))
                .thenReturn(Optional.of(cws));

        when(notificationRepository.existsByAccount_IdAndNotificationTypeAndDeepLinkAndIsDeletedFalse(
                anyInt(), any(), anyString()))
                .thenReturn(false);

        // ===== WHEN =====
        appointmentService.cancelAppointment(appointmentId, accountId);

        // ===== THEN =====
        assertThat(appointment.getAppointmentStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getCancelledBy()).isEqualTo(CancelledBy.MEMBER);
        assertThat(cws.getStatus()).isEqualTo(CoachWorkScheduleStatus.AVAILABLE);
        verify(appointmentRepository).save(appointment);
        verify(coachWorkScheduleRepository).save(cws);
    }

    @Test
    void should_throw_exception_when_cancel_too_late() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int accountId = 100;

        LocalDate appointmentDate = LocalDate.now();
        LocalTime startTime = LocalTime.now().plusMinutes(3); // Chỉ còn 3 phút nữa
        appointment.setDate(appointmentDate);
        appointment.setAppointmentStatus(AppointmentStatus.PENDING);
        cws.setDate(appointmentDate);
        cws.getSlot().setStartTime(startTime);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment));

        when(memberRepository.findByAccountId(accountId))
                .thenReturn(Optional.of(member));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> appointmentService.cancelAppointment(appointmentId, accountId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Too late to cancel");
    }

    @Test
    void should_return_idempotently_when_appointment_already_cancelled() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int accountId = 100;

        appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment));

        when(memberRepository.findByAccountId(accountId))
                .thenReturn(Optional.of(member));

        // ===== WHEN =====
        appointmentService.cancelAppointment(appointmentId, accountId);

        // ===== THEN =====
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void should_throw_exception_when_member_not_authorized_to_cancel() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int accountId = 999; // Khác với member của appointment

        Member otherMember = new Member();
        otherMember.setId(999);
        Account otherAccount = new Account();
        otherAccount.setId(999);
        otherMember.setAccount(otherAccount);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment));

        when(memberRepository.findByAccountId(accountId))
                .thenReturn(Optional.of(otherMember));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> appointmentService.cancelAppointment(appointmentId, accountId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("do not have permission");
    }

    // ========== cancelAppointmentByCoach Tests ==========

    @Test
    void should_cancel_appointment_successfully_when_coach_cancels() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int coachAccountId = 200;

        LocalDate appointmentDate = LocalDate.now().plusDays(1);
        LocalTime startTime = LocalTime.of(10, 0);
        appointment.setDate(appointmentDate);
        appointment.setAppointmentStatus(AppointmentStatus.PENDING);
        cws.setDate(appointmentDate);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment));

        when(coachWorkScheduleRepository.findByCoachIdAndDateAndSlotIdForUpdate(
                anyInt(), any(), anyInt()))
                .thenReturn(Optional.of(cws));

        when(notificationRepository.existsByAccount_IdAndNotificationTypeAndDeepLinkAndIsDeletedFalse(
                anyInt(), any(), anyString()))
                .thenReturn(false);

        // ===== WHEN =====
        appointmentService.cancelAppointmentByCoach(appointmentId, coachAccountId);

        // ===== THEN =====
        assertThat(appointment.getAppointmentStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getCancelledBy()).isEqualTo(CancelledBy.COACH);
        assertThat(cws.getStatus()).isEqualTo(CoachWorkScheduleStatus.UNAVAILABLE);
        verify(appointmentRepository).save(appointment);
        verify(coachWorkScheduleRepository).save(cws);
    }

    @Test
    void should_throw_exception_when_coach_cancel_too_late() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int coachAccountId = 200;

        LocalDate appointmentDate = LocalDate.now();
        LocalTime startTime = LocalTime.now().plusMinutes(20); // Chỉ còn 20 phút (cần > 30 phút)
        appointment.setDate(appointmentDate);
        appointment.setAppointmentStatus(AppointmentStatus.PENDING);
        cws.setDate(appointmentDate);
        cws.getSlot().setStartTime(startTime);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> appointmentService.cancelAppointmentByCoach(appointmentId, coachAccountId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Too late to cancel");
    }

    // ========== getRemainingBookingsForMember Tests ==========

    @Test
    void should_return_remaining_bookings_when_subscription_exists() {
        // ===== GIVEN =====
        int accountId = 100;
        LocalDate startDate = LocalDate.now().minusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(20);

        MembershipSubscription sub = new MembershipSubscription();
        sub.setStartDate(startDate);
        sub.setEndDate(endDate);

        when(memberRepository.findByAccountId(accountId))
                .thenReturn(Optional.of(member));

        when(membershipSubscriptionRepository.findActiveByMemberId(anyInt(), any()))
                .thenReturn(Optional.of(sub));

        when(appointmentRepository.countActiveByMemberIdAndDateBetween(anyInt(), any(), any()))
                .thenReturn(2L); // Đã dùng 2 lượt

        // ===== WHEN =====
        RemainingBookingResponse result = appointmentService.getRemainingBookingsForMember(accountId);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getAllowed()).isGreaterThan(0);
        assertThat(result.getUsed()).isEqualTo(2);
        assertThat(result.getRemaining()).isGreaterThanOrEqualTo(0);
        assertThat(result.getPeriodStart()).isEqualTo(startDate);
        assertThat(result.getPeriodEnd()).isEqualTo(endDate);
    }

    @Test
    void should_return_zero_when_no_active_subscription() {
        // ===== GIVEN =====
        int accountId = 100;

        when(memberRepository.findByAccountId(accountId))
                .thenReturn(Optional.of(member));

        when(membershipSubscriptionRepository.findActiveByMemberId(anyInt(), any()))
                .thenReturn(Optional.empty());

        // ===== WHEN =====
        RemainingBookingResponse result = appointmentService.getRemainingBookingsForMember(accountId);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getAllowed()).isEqualTo(0);
        assertThat(result.getUsed()).isEqualTo(0);
        assertThat(result.getRemaining()).isEqualTo(0);
        assertThat(result.getNote()).contains("No active subscription");
    }

    // ========== completeAppointmentByCoach Tests ==========

    @Test
    void should_complete_appointment_successfully() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int coachAccountId = 200;

        LocalDate appointmentDate = LocalDate.now();
        LocalTime startTime = LocalTime.now().minusMinutes(15); // Đã qua 15 phút (cho phép sau 10 phút)
        appointment.setDate(appointmentDate);
        appointment.setAppointmentStatus(AppointmentStatus.PENDING);
        cws.setDate(appointmentDate);
        cws.getSlot().setStartTime(startTime);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment));

        // ===== WHEN =====
        appointmentService.completeAppointmentByCoach(appointmentId, coachAccountId);

        // ===== THEN =====
        assertThat(appointment.getAppointmentStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void should_throw_exception_when_complete_too_early() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int coachAccountId = 200;

        LocalDate appointmentDate = LocalDate.now();
        LocalTime startTime = LocalTime.now().plusMinutes(5); // Chưa đến giờ bắt đầu
        appointment.setDate(appointmentDate);
        appointment.setAppointmentStatus(AppointmentStatus.PENDING);
        cws.setDate(appointmentDate);
        cws.getSlot().setStartTime(startTime);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> appointmentService.completeAppointmentByCoach(appointmentId, coachAccountId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Too early to complete");
    }

    @Test
    void should_throw_exception_when_complete_cancelled_appointment() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int coachAccountId = 200;

        appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> appointmentService.completeAppointmentByCoach(appointmentId, coachAccountId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot complete a cancelled appointment");
    }

    // ========== generateJoinTokenForAppointment Tests ==========

    @Test
    void should_generate_join_token_successfully() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int accountId = 100;

        LocalDate appointmentDate = LocalDate.now();
        LocalTime startTime = LocalTime.now().minusMinutes(2); // Đang trong join window
        LocalTime endTime = startTime.plusMinutes(30);
        
        appointment.setDate(appointmentDate);
        appointment.setAppointmentStatus(AppointmentStatus.PENDING);
        cws.setDate(appointmentDate);
        cws.getSlot().setStartTime(startTime);
        cws.getSlot().setEndTime(endTime);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment));

        when(agoraService.generateRtcToken(anyString(), anyInt(), anyInt()))
                .thenReturn("mock-token");

        // ===== WHEN =====
        JoinTokenResponse result = appointmentService.generateJoinTokenForAppointment(appointmentId, accountId);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getChannel()).contains("appointment_");
        assertThat(result.getToken()).isEqualTo("mock-token");
        verify(agoraService).generateRtcToken(anyString(), anyInt(), anyInt());
    }

    @Test
    void should_throw_exception_when_join_too_early() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int accountId = 100;

        LocalDate appointmentDate = LocalDate.now().plusDays(1);
        LocalTime startTime = LocalTime.of(10, 0);
        appointment.setDate(appointmentDate);
        appointment.setAppointmentStatus(AppointmentStatus.PENDING);
        cws.setDate(appointmentDate);
        cws.getSlot().setStartTime(startTime);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> appointmentService.generateJoinTokenForAppointment(appointmentId, accountId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Too early to join");
    }

    @Test
    void should_throw_exception_when_join_cancelled_appointment() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int accountId = 100;

        appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> appointmentService.generateJoinTokenForAppointment(appointmentId, accountId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Appointment is cancelled");
    }

    // ========== getAppointmentDetailForPrincipal Tests ==========

    @Test
    void should_get_appointment_detail_for_member() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int accountId = 100;

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment));

        AppointmentResponse response = new AppointmentResponse();
        when(appointmentMapper.toResponseWithRuntime(any(), anyString()))
                .thenReturn(response);

        // ===== WHEN =====
        AppointmentResponse result = appointmentService.getAppointmentDetailForPrincipal(appointmentId, accountId);

        // ===== THEN =====
        assertThat(result).isNotNull();
        verify(appointmentMapper).toResponseWithRuntime(any(), anyString());
    }

    @Test
    void should_throw_exception_when_unauthorized_to_view() {
        // ===== GIVEN =====
        int appointmentId = 50;
        int accountId = 999; // Không phải member hoặc coach của appointment

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> appointmentService.getAppointmentDetailForPrincipal(appointmentId, accountId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("do not have permission");
    }

    // ========== HELPER METHODS ==========

    private MembershipSubscription mockSubscription() {
        MembershipSubscription sub = new MembershipSubscription();
        sub.setStartDate(LocalDate.now().minusDays(1));
        sub.setEndDate(LocalDate.now().plusDays(30));
        return sub;
    }
}
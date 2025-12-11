package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.AppointmentRequest;
import com.smartquit.smartquitiot.dto.response.AppointmentResponse;
import com.smartquit.smartquitiot.dto.response.JoinTokenResponse;
import com.smartquit.smartquitiot.dto.response.RemainingBookingResponse;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.AppointmentStatus;
import com.smartquit.smartquitiot.enums.CancelledBy;
import com.smartquit.smartquitiot.enums.CoachWorkScheduleStatus;
import com.smartquit.smartquitiot.enums.NotificationType;
import com.smartquit.smartquitiot.mapper.AppointmentMapper;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.AgoraService;
import com.smartquit.smartquitiot.service.AppointmentService;
import com.smartquit.smartquitiot.service.NotificationService;
import com.smartquit.smartquitiot.specifications.AppointmentSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation cho Appointment.
 * - Chú thích bằng tiếng Việt.
 * - Các thông báo lỗi / exception messages bằng tiếng Anh (theo yêu cầu).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final CoachWorkScheduleRepository coachWorkScheduleRepository;
    private final MemberRepository memberRepository;
    private final AppointmentMapper appointmentMapper;
    private final AgoraService agoraService;
    private final MembershipSubscriptionRepository membershipSubscriptionRepository; // NEW
    private final FeedbackRepository feedbackRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private static final int BOOKINGS_PER_30D = 4;

    /**
     * Member đặt lịch hẹn với coach.
     * - Trước khi book sẽ kiểm tra member còn lượt trong subscription hiện tại không.
     */
    @Override
    @Transactional
    public AppointmentResponse bookAppointment(int accountId, AppointmentRequest request) {

        // tìm member theo accountId
        Member member = memberRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        var memberId = member.getId();
        var date = request.getDate();
        var coachId = request.getCoachId();
        var slotId = request.getSlotId();

        // --- CHECK 1: kiểm tra còn lượt booking hay không (dựa trên subscription active)
        RemainingBookingResponse remaining = getRemainingBookingsForMember(accountId);
        if (remaining == null) {
            throw new IllegalStateException("No active subscription");
        }
        if (remaining.getRemaining() <= 0) {
            throw new IllegalStateException("No remaining booking available in your subscription period");
        }

        // --- CHECK 2: kiểm tra trùng thời gian với appointment khác (cùng member, cùng slot, khác coach)
        // Business rule: Không cho phép đặt lịch trùng thời gian
        List<Appointment> overlappingAppointments = appointmentRepository.findOverlappingAppointments(
                memberId, date, slotId, coachId);
        
        if (!overlappingAppointments.isEmpty()) {
            // Lấy thông tin slot để hiển thị thời gian trong message
            String slotTimeInfo = "";
            if (!overlappingAppointments.isEmpty() && overlappingAppointments.get(0).getCoachWorkSchedule() != null 
                    && overlappingAppointments.get(0).getCoachWorkSchedule().getSlot() != null) {
                Slot slot = overlappingAppointments.get(0).getCoachWorkSchedule().getSlot();
                slotTimeInfo = String.format(" at %s", slot.getStartTime());
            }
            
            // Tạo danh sách tên coach bị trùng
            String conflictingCoaches = overlappingAppointments.stream()
                    .map(a -> {
                        if (a.getCoach() != null) {
                            String fn = a.getCoach().getFirstName() != null ? a.getCoach().getFirstName() : "";
                            String ln = a.getCoach().getLastName() != null ? a.getCoach().getLastName() : "";
                            return (fn + " " + ln).trim();
                        }
                        return "Unknown Coach";
                    })
                    .filter(name -> !name.isEmpty())
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(", "));
            
            String errorMessage = String.format(
                    "You already have an appointment scheduled for %s%s with coach(s): %s. " +
                    "Cannot book overlapping appointments.",
                    date, slotTimeInfo, conflictingCoaches
            );
            
            // Không cho phép đặt lịch trùng thời gian
            throw new IllegalStateException(errorMessage);
        }

        // Kiểm tra slot đã được đặt chưa (by coachId, slotId, date, miễn không phải cancel)
        if (appointmentRepository.existsActiveByCoachSlotDate(coachId, slotId, date)) {
            throw new IllegalStateException("This slot has already been booked");
        }

        // Kiểm tra slot có trong lịch làm việc không
        CoachWorkSchedule cws = coachWorkScheduleRepository
                .findByCoachIdAndDateAndSlotIdForUpdate(coachId, date, slotId)
                .orElseThrow(() -> new IllegalArgumentException("Coach does not have this slot in schedule"));

        if (cws.getStatus() != CoachWorkScheduleStatus.AVAILABLE) {
            throw new IllegalStateException("This slot is not available");
        }

        // Cập nhật trạng thái CWS sang BOOKED
        cws.setStatus(CoachWorkScheduleStatus.BOOKED);
        coachWorkScheduleRepository.save(cws);

        // Tạo appointment mới, trạng thái PENDING
        Appointment appointment = new Appointment();
        appointment.setAppointmentStatus(AppointmentStatus.PENDING);
        appointment.setCoach(cws.getCoach());
        appointment.setMember(member);
        appointment.setDate(cws.getDate());
        appointment.setName("Appointment with coach " + (cws.getCoach().getLastName() != null ? cws.getCoach().getLastName() : ""));
        appointment.setCoachWorkSchedule(cws);
        appointmentRepository.save(appointment);

        log.info("Member(accountId={}) (memberId={}) booked slot {} with coach {} on date {}",
                accountId, memberId, slotId, coachId, date);

        // Phần notification
        try {
            Account coachAccount = cws.getCoach().getAccount();
            if (coachAccount != null) {
                String deepLink = "smartquit://appointment/" + appointment.getId();
                String url = "appointments/" + appointment.getId();

                boolean already = notificationRepository
                        .existsByAccount_IdAndNotificationTypeAndDeepLinkAndIsDeletedFalse(
                                coachAccount.getId(),
                                NotificationType.APPOINTMENT_BOOKED,
                                deepLink
                        );
                if (!already) {
                    String title = "New booking: appointment #" + appointment.getId();
                    String content = String.format("Member %s requested an appointment on %s at %s",
                            (member.getFirstName() != null ? member.getFirstName() : "Member"),
                            appointment.getDate(),
                            (cws.getSlot() != null ? cws.getSlot().getStartTime().toString() : "unknown time")
                    );
                    try {
                        notificationService.saveAndPublish(
                                coachAccount,
                                NotificationType.APPOINTMENT_BOOKED,
                                title,
                                content,
                                null,
                                url,
                                deepLink
                        );
                    } catch (Exception ex) {
                        log.warn("Failed to publish appointment booked notification for appointment {}: {}", appointment.getId(), ex.getMessage());
                    }
                } else {
                    log.debug("Booked noti already exists for appointment {}", appointment.getId());
                }
            } else {
                log.warn("Coach account missing for appointment {} — skip booked notification", appointment.getId());
            }
        } catch (Exception ex) {
            log.error("Error while sending booked notification for appointment {}: {}", appointment.getId(), ex.getMessage(), ex);
        }


        return appointmentMapper.toResponse(appointment);
    }


    /**
     * Member huỷ appointment.
     * Quy tắc: member cancel -> appointment still counts as used (policy).
     * Thời hạn huỷ: member phải huỷ trước 5 phút so với bắt đầu slot.
     */
    @Override
    @Transactional
    public void cancelAppointment(int appointmentId, int accountId) {
        // kiểm tra appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        // kiểm tra member account
        Member member = memberRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (appointment.getMember() == null || appointment.getMember().getId() != member.getId()) {
            throw new SecurityException("You do not have permission to cancel this appointment");
        }

        // nếu đã bị huỷ rồi thì idempotent return
        if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
            log.info("Appointment {} already in CANCELLED state", appointmentId);
            return;
        }

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

        CoachWorkSchedule linkedCws = appointment.getCoachWorkSchedule();
        CoachWorkSchedule cwsForTime = linkedCws;
        if (cwsForTime == null && appointment.getCoach() != null && appointment.getCoachWorkSchedule() != null) {
            // fallback: try fetch by coach/date/slot
            cwsForTime = coachWorkScheduleRepository.findByCoachIdAndDateAndSlotIdForUpdate(
                    appointment.getCoach().getId(),
                    appointment.getDate(),
                    appointment.getCoachWorkSchedule().getSlot() != null ? appointment.getCoachWorkSchedule().getSlot().getId() : -1
            ).orElse(null);
        }

        if (cwsForTime == null || cwsForTime.getSlot() == null) {
            throw new IllegalStateException("Cannot determine slot information to validate cancellation window");
        }

        LocalDate apDate = appointment.getDate();
        LocalTime startTime = cwsForTime.getSlot().getStartTime();
        ZonedDateTime startZ = LocalDateTime.of(apDate, startTime).atZone(zone);
        ZonedDateTime nowZ = ZonedDateTime.now(zone);
        ZonedDateTime memberLimit = startZ.minusMinutes(5);

        if (!nowZ.isBefore(memberLimit)) {
            throw new IllegalStateException("Too late to cancel. Member must cancel at least 5 minutes before slot start");
        }

        if (linkedCws != null && linkedCws.getSlot() != null) {
            CoachWorkSchedule stored = coachWorkScheduleRepository
                    .findByCoachIdAndDateAndSlotIdForUpdate(
                            appointment.getCoach().getId(),
                            appointment.getDate(),
                            linkedCws.getSlot().getId()
                    ).orElse(null);

            if (stored != null) {
                stored.setStatus(CoachWorkScheduleStatus.AVAILABLE);
                coachWorkScheduleRepository.save(stored);
            } else {
                log.warn("CWS not found by forUpdate for appointment {} — fallback skipped", appointmentId);
            }
        } else {
            log.warn("Appointment {} has no linked CoachWorkSchedule when member cancels", appointmentId);
        }

        appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledBy(CancelledBy.MEMBER);
        appointment.setCancelledAt(LocalDateTime.now());

        appointmentRepository.save(appointment);

        log.info("Member(accountId={}) cancelled appointment {}", accountId, appointmentId);
        // Phần thông báo
        try {
            CoachWorkSchedule linked = appointment.getCoachWorkSchedule();
            if (linked != null && linked.getCoach() != null && linked.getCoach().getAccount() != null) {
                Account coachAccount = linked.getCoach().getAccount();
                String deepLink = "smartquit://appointment/" + appointment.getId();
                String url = "appointments/" + appointment.getId();

                boolean already = notificationRepository
                        .existsByAccount_IdAndNotificationTypeAndDeepLinkAndIsDeletedFalse(
                                coachAccount.getId(),
                                NotificationType.APPOINTMENT_CANCELLED,
                                deepLink
                        );
                if (!already) {
                    String title = "Appointment cancelled: #" + appointment.getId();
                    String content = String.format("Member %s cancelled the appointment scheduled at %s",
                            (appointment.getMember() != null && appointment.getMember().getFirstName() != null) ? appointment.getMember().getFirstName() : "Member",
                            (linked.getSlot() != null ? linked.getSlot().getStartTime().toString() : "unknown time")
                    );
                    try {
                        notificationService.saveAndPublish(
                                coachAccount,
                                NotificationType.APPOINTMENT_CANCELLED,
                                title,
                                content,
                                null,
                                url,
                                deepLink
                        );
                    } catch (Exception ex) {
                        log.warn("Failed to publish appointment cancelled (member) notification for appointment {}: {}", appointment.getId(), ex.getMessage());
                    }
                } else {
                    log.debug("Cancel noti already exists for appointment {}", appointment.getId());
                }
            } else {
                log.warn("No coach account found for appointment {} — skip cancel notification", appointment.getId());
            }
        } catch (Exception ex) {
            log.error("Error while sending cancel (member) notification for appointment {}: {}", appointment.getId(), ex.getMessage(), ex);
        }

    }

    /**
     * Coach huỷ appointment của họ.
     * Quy tắc: coach cancel -> appointment refunded (không tính là used)
     * Coach phải huỷ ít nhất 30 phút trước slot start.
     */
    @Override
    @Transactional
    public void cancelAppointmentByCoach(int appointmentId, int coachAccountId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        if (appointment.getCoach() == null
                || appointment.getCoach().getAccount() == null
                || appointment.getCoach().getAccount().getId() != coachAccountId) {
            throw new SecurityException("You do not have permission to cancel this appointment");
        }

        // Idempotent
        if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
            log.info("Appointment {} already in CANCELLED state (coach cancel)", appointmentId);
            return;
        }

        CoachWorkSchedule linkedCws = appointment.getCoachWorkSchedule();
        if (linkedCws == null || linkedCws.getSlot() == null) {
            throw new IllegalStateException("Cannot determine slot information to validate cancellation window");
        }

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate apDate = appointment.getDate();
        LocalTime startTime = linkedCws.getSlot().getStartTime();
        ZonedDateTime startZ = LocalDateTime.of(apDate, startTime).atZone(zone);
        ZonedDateTime nowZ = ZonedDateTime.now(zone);
        ZonedDateTime coachLimit = startZ.minusMinutes(30);

        if (!nowZ.isBefore(coachLimit)) {
            throw new IllegalStateException("Too late to cancel. Coach must cancel at least 30 minutes before slot start");
        }

        CoachWorkSchedule stored = coachWorkScheduleRepository
                .findByCoachIdAndDateAndSlotIdForUpdate(
                        appointment.getCoach().getId(),
                        appointment.getDate(),
                        linkedCws.getSlot().getId()
                ).orElse(null);

        if (stored != null) {
            stored.setStatus(CoachWorkScheduleStatus.UNAVAILABLE);
            coachWorkScheduleRepository.save(stored);
        } else {
            log.warn("CWS not found by forUpdate for appointment {} — using linkedCws", appointmentId);
            linkedCws.setStatus(CoachWorkScheduleStatus.UNAVAILABLE);
            coachWorkScheduleRepository.save(linkedCws);
        }

        appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledBy(CancelledBy.COACH);
        appointment.setCancelledAt(LocalDateTime.now());

        appointmentRepository.save(appointment);

        log.info("Coach(accountId={}) cancelled appointment {} — slot marked UNAVAILABLE", coachAccountId, appointmentId);
        // Phần thông báo
        try {
            if (appointment.getMember() != null && appointment.getMember().getAccount() != null) {
                Account memberAccount = appointment.getMember().getAccount();
                String deepLink = "smartquit://appointment/" + appointment.getId();
                String url = "appointments/" + appointment.getId();

                boolean already = notificationRepository
                        .existsByAccount_IdAndNotificationTypeAndDeepLinkAndIsDeletedFalse(
                                memberAccount.getId(),
                                NotificationType.APPOINTMENT_CANCELLED,
                                deepLink
                        );
                if (!already) {
                    String title = "Your appointment was cancelled";
                    String content = String.format("Coach %s cancelled appointment #%d scheduled at %s. We will assist you to rebook.",
                            (appointment.getCoach()!=null && appointment.getCoach().getLastName()!=null) ? appointment.getCoach().getLastName() : "Coach",
                            appointment.getId(),
                            (appointment.getCoachWorkSchedule()!=null && appointment.getCoachWorkSchedule().getSlot()!=null) ? appointment.getCoachWorkSchedule().getSlot().getStartTime().toString() : "unknown time"
                    );
                    try {
                        notificationService.saveAndPublish(
                                memberAccount,
                                NotificationType.APPOINTMENT_CANCELLED,
                                title,
                                content,
                                null,
                                url,
                                deepLink
                        );
                    } catch (Exception ex) {
                        log.warn("Failed to publish appointment cancelled (coach) notification for appointment {}: {}", appointment.getId(), ex.getMessage());
                    }
                } else {
                    log.debug("Cancel noti already exists for appointment {} to member", appointment.getId());
                }
            } else {
                log.warn("Member account missing for appointment {} — skip coach-cancel notification", appointment.getId());
            }
        } catch (Exception ex) {
            log.error("Error while sending cancel (coach) notification for appointment {}: {}", appointment.getId(), ex.getMessage(), ex);
        }

    }

    /**
     * Lấy danh sách appointment cho member theo filter; kèm runtime status tính từ slot.
     */
    @Override
    public List<AppointmentResponse> getAppointmentsByMemberAccountId(int memberAccountId,
                                                                      String statusFilter,
                                                                      String dateFilter,
                                                                      int page,
                                                                      int size) {

        Member member = memberRepository.findByAccountId(memberAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        int memberId = member.getId();

        List<Appointment> all = appointmentRepository.findAllByMemberId(memberId);

        // parse status filter
        AppointmentStatus parsedStatus = null;
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                parsedStatus = AppointmentStatus.valueOf(statusFilter.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status filter: " + statusFilter);
            }
        }

        // parse date filter - expects yyyy-MM-dd
        LocalDate parsedDate = null;
        if (dateFilter != null && !dateFilter.isBlank()) {
            try {
                parsedDate = LocalDate.parse(dateFilter.trim());
            } catch (DateTimeException e) {
                throw new IllegalArgumentException("Invalid date filter, expected yyyy-MM-dd");
            }
        }

        final AppointmentStatus statusFinal = parsedStatus;
        final LocalDate dateFinal = parsedDate;

        var converted = all.stream()
                .filter(a -> {
                    if (statusFinal != null && a.getAppointmentStatus() != statusFinal) return false;
                    if (dateFinal != null && (a.getDate() == null || !a.getDate().equals(dateFinal))) return false;
                    return true;
                })
                .map(a -> {
                    String runtimeStatus;
                    if (a.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
                        runtimeStatus = "CANCELLED";
                    } else if (a.getAppointmentStatus() == AppointmentStatus.COMPLETED) {
                        runtimeStatus = "COMPLETED";
                    } else {
                        CoachWorkSchedule cws = a.getCoachWorkSchedule();
                        runtimeStatus = (cws != null && cws.getSlot() != null) ? calculateRuntimeStatus(cws) : "UNKNOWN";
                    }
                    return appointmentMapper.toResponseWithRuntime(a, runtimeStatus);
                })
                .toList();

        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        int from = page * size;
        if (from >= converted.size()) return List.of();
        int to = Math.min(converted.size(), from + size);

        // create a modifiable copy of the sublist so we can set hasRated safely
        List<AppointmentResponse> pageList = new ArrayList<>(converted.subList(from, to));

        // simple per-appointment check (fine for small scale)
        for (AppointmentResponse r : pageList) {
            try {
                boolean exists = feedbackRepository.existsByAppointment_Id(r.getAppointmentId());
                r.setHasRated(exists);
            } catch (Exception e) {
                // defensive: if repo fails, leave hasRated = false and continue
                log.warn("Failed to check feedback for appointment {}: {}", r.getAppointmentId(), e.getMessage());
                r.setHasRated(false);
            }
        }

        return pageList;
    }

    /**
     * Lấy danh sách appointment cho coach theo filter; kèm runtime status và thông tin member.
     */
    @Override
    public List<AppointmentResponse> getAppointmentsByCoachAccountId(int coachAccountId,
                                                                     String statusFilter,
                                                                     String dateFilter,
                                                                     int page,
                                                                     int size) {

        // fetch appointments by coach's account id
        List<Appointment> all = appointmentRepository.findAllByCoachAccountId(coachAccountId);

        AppointmentStatus parsedStatus = null;
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                parsedStatus = AppointmentStatus.valueOf(statusFilter.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status filter: " + statusFilter);
            }
        }

        LocalDate parsedDate = null;
        if (dateFilter != null && !dateFilter.isBlank()) {
            try {
                parsedDate = LocalDate.parse(dateFilter.trim());
            } catch (DateTimeException e) {
                throw new IllegalArgumentException("Invalid date filter, expected yyyy-MM-dd");
            }
        }

        final AppointmentStatus statusFinal = parsedStatus;
        final LocalDate dateFinal = parsedDate;

        var converted = all.stream()
                .filter(a -> {
                    if (a.getCoach() == null || a.getCoach().getAccount() == null) return false;
                    if (a.getCoach().getAccount().getId() != coachAccountId) return false;
                    if (statusFinal != null && a.getAppointmentStatus() != statusFinal) return false;
                    if (dateFinal != null && (a.getDate() == null || !a.getDate().equals(dateFinal))) return false;
                    return true;
                })
                .map(a -> {
                    String runtimeStatus;
                    if (a.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
                        runtimeStatus = "CANCELLED";
                    } else if (a.getAppointmentStatus() == AppointmentStatus.COMPLETED) {
                        runtimeStatus = "COMPLETED";
                    }else {
                        CoachWorkSchedule cws = a.getCoachWorkSchedule();
                        runtimeStatus = (cws != null && cws.getSlot() != null) ? calculateRuntimeStatus(cws) : "UNKNOWN";
                    }

                    AppointmentResponse resp = appointmentMapper.toResponseWithRuntime(a, runtimeStatus);

                    if (a.getMember() != null) {
                        resp.setMemberId(a.getMember().getId());
                        String mf = a.getMember().getFirstName() != null ? a.getMember().getFirstName() : "";
                        String ml = a.getMember().getLastName() != null ? a.getMember().getLastName() : "";
                        resp.setMemberName((mf + " " + ml).trim());
                    }

                    return resp;
                })
                .toList();

        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        int from = page * size;
        if (from >= converted.size()) return List.of();
        int to = Math.min(converted.size(), from + size);
        return converted.subList(from, to);
    }

    /**
     * Tính runtime status dựa trên CoachWorkSchedule slot times.
     */
    private String calculateRuntimeStatus(CoachWorkSchedule cws) {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

        LocalDate date = cws.getDate();
        LocalTime start = cws.getSlot().getStartTime();
        LocalTime end = cws.getSlot().getEndTime();

        LocalDateTime startDt = LocalDateTime.of(date, start);
        LocalDateTime endDt = LocalDateTime.of(date, end);

        ZonedDateTime startZ = startDt.atZone(zone);
        ZonedDateTime endZ = endDt.atZone(zone);
        Instant nowInstant = Instant.now(); // UTC instant
        ZonedDateTime nowZ = ZonedDateTime.ofInstant(nowInstant, zone);

        if (nowZ.isBefore(startZ)) {
            return "PENDING";
        } else if (nowZ.isBefore(endZ)) {
            return "IN_PROGRESS";
        } else {
            return "COMPLETED";
        }
    }

    /**
     * Lấy chi tiết appointment cho caller (member hoặc coach) sau khi kiểm tra quyền.
     */
    @Override
    public AppointmentResponse getAppointmentDetailForPrincipal(int appointmentId, int accountId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        if (accountId <= 0) {
            throw new IllegalArgumentException("accountId missing from token — cannot verify ownership");
        }

        boolean isMemberCaller = appointment.getMember() != null
                && appointment.getMember().getAccount() != null
                && appointment.getMember().getAccount().getId() == accountId;

        boolean isCoachCaller = appointment.getCoach() != null
                && appointment.getCoach().getAccount() != null
                && appointment.getCoach().getAccount().getId() == accountId;

        if (!isMemberCaller && !isCoachCaller) {
            throw new SecurityException("You do not have permission to view this appointment");
        }

        String runtimeStatus;
        if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
            runtimeStatus = "CANCELLED";
        } else if (appointment.getAppointmentStatus() == AppointmentStatus.COMPLETED) {
            runtimeStatus = "COMPLETED";
        } else {
            CoachWorkSchedule cws = appointment.getCoachWorkSchedule();
            runtimeStatus = (cws != null && cws.getSlot() != null) ? calculateRuntimeStatus(cws) : "UNKNOWN";
        }

        AppointmentResponse resp = appointmentMapper.toResponseWithRuntime(appointment, runtimeStatus);

        if (appointment.getMember() != null) {
            resp.setMemberId(appointment.getMember().getId());
            String mf = appointment.getMember().getFirstName() != null ? appointment.getMember().getFirstName() : "";
            String ml = appointment.getMember().getLastName() != null ? appointment.getMember().getLastName() : "";
            resp.setMemberName((mf + " " + ml).trim());
        }

        return resp;
    }

    /**
     * Tạo join token (Agora) cho appointment, kiểm tra quyền và join window.
     */
    @Override
    @Transactional
    public JoinTokenResponse generateJoinTokenForAppointment(int appointmentId, int accountId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        boolean isMemberCaller = appointment.getMember() != null
                && appointment.getMember().getAccount() != null
                && appointment.getMember().getAccount().getId() == accountId;

        boolean isCoachCaller = appointment.getCoach() != null
                && appointment.getCoach().getAccount() != null
                && appointment.getCoach().getAccount().getId() == accountId;

        if (!isMemberCaller && !isCoachCaller) {
            throw new SecurityException("You do not have permission to join this meeting");
        }

        if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Appointment is cancelled");
        }

        CoachWorkSchedule cws = appointment.getCoachWorkSchedule();
        if (cws == null || cws.getSlot() == null) {
            throw new IllegalStateException("Cannot determine slot information for this appointment");
        }

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate apDate = appointment.getDate();
        LocalTime start = cws.getSlot().getStartTime();
        LocalTime end = cws.getSlot().getEndTime();

        ZonedDateTime windowStartZ = LocalDateTime.of(apDate, start).minusMinutes(5).atZone(zone);
        ZonedDateTime windowEndZ   = LocalDateTime.of(apDate, end).plusMinutes(5).atZone(zone);
        ZonedDateTime nowZ = ZonedDateTime.now(zone);

        if (nowZ.isBefore(windowStartZ)) {
            throw new IllegalStateException("Too early to join. Join window starts at: " + windowStartZ.toString());
        }
        if (nowZ.isAfter(windowEndZ)) {
            throw new IllegalStateException("Join window has already closed");
        }

        long rawTtlSeconds = Duration.between(Instant.now(), windowEndZ.toInstant()).getSeconds();
        long ttlSeconds = Math.max(30, rawTtlSeconds);

        final long MAX_TTL = 86_400L; // 24 hours
        long ttlToUse = Math.min(ttlSeconds, MAX_TTL);

        int ttlInt = (int) ttlToUse;

        String channel = "appointment_" + appointmentId;

        int randomUid = java.util.concurrent.ThreadLocalRandom.current().nextInt(100_000, 2_000_000);

        log.info("Creating join token for appointment={} by accountId={} (ttl={}s) -> uid={}", appointmentId, accountId, ttlInt, randomUid);

        String token = agoraService.generateRtcToken(channel, randomUid, ttlInt);
        long expiresAt = Instant.now().getEpochSecond() + ttlInt;

        return new JoinTokenResponse(channel, token, randomUid, expiresAt, ttlInt);
    }

    /**
     * Lấy số lượt đặt hẹn còn lại cho member dựa trên subscription active hiện tại.
     * - allowed = floor((days_in_period * 4) / 30)
     * - used = count appointment where:
     *      appointment.date BETWEEN start..end AND
     *      (appointmentStatus <> CANCELLED OR cancelledBy = MEMBER)
     * - remaining = max(0, allowed - used)
     *
     */
    @Override
    public RemainingBookingResponse getRemainingBookingsForMember(int memberAccountId) {
        Member member = memberRepository.findByAccountId(memberAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        int memberId = member.getId();

        LocalDate today = LocalDate.now();

        var subOpt = membershipSubscriptionRepository.findActiveByMemberId(memberId, today);
        if (subOpt.isEmpty()) {
            return RemainingBookingResponse.builder()
                    .allowed(0)
                    .used(0)
                    .remaining(0)
                    .periodStart(null)
                    .periodEnd(null)
                    .note("No active subscription")
                    .build();
        }

        MembershipSubscription sub = subOpt.get();
        LocalDate start = sub.getStartDate();
        LocalDate end = sub.getEndDate();

        long days = ChronoUnit.DAYS.between(start, end) + 1;
        int allowed = (int) Math.floor((days * BOOKINGS_PER_30D) / 30.0);

        long usedLong = appointmentRepository.countActiveByMemberIdAndDateBetween(memberId, start, end);
        int used = (int) Math.min(usedLong, Integer.MAX_VALUE);

        int remaining = Math.max(0, allowed - used);

        return RemainingBookingResponse.builder()
                .allowed(allowed)
                .used(used)
                .remaining(remaining)
                .periodStart(start)
                .periodEnd(end)
                .note("Counting non-cancelled appointments and member-cancelled appointments as used; coach-cancelled appointments are refunded.")
                .build();
    }

    @Override
    @Transactional
    public void completeAppointmentByCoach(int appointmentId, int coachAccountId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        // quyền: phải là coach phụ trách appointment
        if (appointment.getCoach() == null
                || appointment.getCoach().getAccount() == null
                || appointment.getCoach().getAccount().getId() != coachAccountId) {
            throw new SecurityException("You do not have permission to complete this appointment");
        }

        // nếu đã CANCELLED thì không cho
        if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Cannot complete a cancelled appointment");
        }

        // nếu đã COMPLETED thì idempotent return
        if (appointment.getAppointmentStatus() == AppointmentStatus.COMPLETED) {
            log.info("Appointment {} is already COMPLETED", appointmentId);
            return;
        }

        // lấy thông tin slot để kiểm tra thời gian
        CoachWorkSchedule linkedCws = appointment.getCoachWorkSchedule();
        if (linkedCws == null || linkedCws.getSlot() == null) {
            throw new IllegalStateException("Cannot determine slot information to validate manual completion window");
        }

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate apDate = appointment.getDate();
        LocalTime startTime = linkedCws.getSlot().getStartTime();
        ZonedDateTime startZ = LocalDateTime.of(apDate, startTime).atZone(zone);

        // business rule: cho phép completed sau 10' slot start
        final int MANUAL_COMPLETE_MINUTES = 10;
        ZonedDateTime allowedCompleteAt = startZ.plusMinutes(MANUAL_COMPLETE_MINUTES);
        ZonedDateTime nowZ = ZonedDateTime.now(zone);

        if (nowZ.isBefore(allowedCompleteAt)) {
            throw new IllegalStateException("Too early to complete. Manual completion allowed from: " + allowedCompleteAt.toString());
        }

        appointment.setAppointmentStatus(AppointmentStatus.COMPLETED);


        appointmentRepository.save(appointment);

        log.info("Coach(accountId={}) manually completed appointment {}", coachAccountId, appointmentId);
    }

    @Override
    @Transactional
    public void addSnapshots(int appointmentId, int accountId, List<String> urls) {

        Appointment ap = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        boolean isMember = ap.getMember() != null &&
                ap.getMember().getAccount().getId() == accountId;

        boolean isCoach = ap.getCoach() != null &&
                ap.getCoach().getAccount().getId() == accountId;

        if (!isMember && !isCoach) {
            throw new SecurityException("You do not have permission to upload snapshots");
        }

        if (urls != null) {
            ap.getSnapshotUrls().addAll(urls);
        }

        appointmentRepository.save(ap);
    }

    @Override
    public List<String> getSnapshots(int appointmentId, int accountId) {

        Appointment ap = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        boolean isMember = ap.getMember() != null &&
                ap.getMember().getAccount().getId() == accountId;

        boolean isCoach = ap.getCoach() != null &&
                ap.getCoach().getAccount().getId() == accountId;

        if (!isMember && !isCoach) {
            throw new SecurityException("You do not have permission to view these snapshots");
        }

        return ap.getSnapshotUrls();
    }

    @Override
    public Page<AppointmentResponse> getAllAppointments(int page, int size, AppointmentStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Specification<Appointment> spec = Specification.allOf(AppointmentSpecification.hasStatus(status));
        Page<Appointment> apPage = appointmentRepository.findAll(spec,pageable);
        return apPage.map(appointmentMapper::toResponse);
    }

    @Override
    @Transactional
    public void reassignAppointment(int appointmentId, int targetCoachId) {
        Appointment ap = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        if (ap.getAppointmentStatus() != AppointmentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING appointments can be reassigned");
        }

        CoachWorkSchedule currentCws = ap.getCoachWorkSchedule();
        if (currentCws == null || currentCws.getSlot() == null || ap.getDate() == null) {
            throw new IllegalStateException("Cannot determine appointment's date/slot to perform reassignment");
        }

        LocalDate date = ap.getDate();
        int slotId = currentCws.getSlot().getId();

        // Lưu thông tin old coach/account trước khi thay đổi
        Coach oldCoach = ap.getCoach();
        Account oldCoachAccount = (oldCoach != null) ? oldCoach.getAccount() : null;

        // 1) Lock target CWS row for update
        CoachWorkSchedule targetCws = coachWorkScheduleRepository
                .findByCoachIdAndDateAndSlotIdForUpdate(targetCoachId, date, slotId)
                .orElseThrow(() -> new IllegalArgumentException("Target coach does not have this slot in schedule"));

        // 2) ensure target is AVAILABLE
        if (targetCws.getStatus() != CoachWorkScheduleStatus.AVAILABLE) {
            throw new IllegalStateException("Target coach is not available for the specified slot");
        }

        // 3) double-check target slot not already booked (safety)
        if (appointmentRepository.existsActiveByCoachSlotDate(targetCoachId, slotId, date)) {
            throw new IllegalStateException("Target coach already has a booking for this slot");
        }

        // 4) Lock current CWS for update (try to fetch FOR UPDATE)
        CoachWorkSchedule lockedCurrentCws = coachWorkScheduleRepository
                .findByCoachIdAndDateAndSlotIdForUpdate(oldCoach != null ? oldCoach.getId() : -1, date, slotId)
                .orElse(null);

        // 5) perform the swap:
        // - mark current CWS as UNAVAILABLE (because coach A is busy)
        // - mark targetCws as BOOKED
        if (lockedCurrentCws != null) {
            lockedCurrentCws.setStatus(CoachWorkScheduleStatus.UNAVAILABLE);
            coachWorkScheduleRepository.save(lockedCurrentCws);
        } else {
            // fallback: if we couldn't lock currentCws by coachId, try using the attached entity
            currentCws.setStatus(CoachWorkScheduleStatus.UNAVAILABLE);
            coachWorkScheduleRepository.save(currentCws);
        }

        targetCws.setStatus(CoachWorkScheduleStatus.BOOKED);
        coachWorkScheduleRepository.save(targetCws);

        // 6) update appointment -> new coach + new coachWorkSchedule
        Coach newCoach = targetCws.getCoach();
        if (newCoach == null) {
            throw new IllegalStateException("Target coach entity missing");
        }

        ap.setCoach(newCoach);
        ap.setCoachWorkSchedule(targetCws);

        appointmentRepository.save(ap);

        log.info("Reassigned appointment {} -> coach {} for date {} slot {} (old coach set to UNAVAILABLE)",
                appointmentId, targetCoachId, date, slotId);

        // 7) notifications: notify new coach + member + notify old coach that they were unassigned
        try {
            Account memberAccount = ap.getMember() != null ? ap.getMember().getAccount() : null;
            Account newCoachAccount = newCoach.getAccount();

            String deepLink = "smartquit://appointment/" + ap.getId();
            String url = "appointments/" + ap.getId();

            // notify new coach
            if (newCoachAccount != null) {
                notificationService.saveAndPublish(
                        newCoachAccount,
                        NotificationType.APPOINTMENT_BOOKED,
                        "You have been assigned to an appointment",
                        String.format("You were assigned to appointment #%d at %s on %s", ap.getId(), targetCws.getSlot().getStartTime(), date),
                        null,
                        url,
                        deepLink
                );
            }

            // notify member
            if (memberAccount != null) {
                notificationService.saveAndPublish(
                        memberAccount,
                        NotificationType.APPOINTMENT_BOOKED,
                        "Your appointment has been reassigned",
                        String.format("Your appointment #%d was reassigned to coach %s %s at %s on %s",
                                ap.getId(),
                                newCoach.getFirstName() != null ? newCoach.getFirstName() : "",
                                newCoach.getLastName() != null ? newCoach.getLastName() : "",
                                targetCws.getSlot().getStartTime(),
                                date),
                        null,
                        url,
                        deepLink
                );
            }

            // notify old coach that they are unassigned (use CANCELLED type or custom type)
            if (oldCoachAccount != null) {
                notificationService.saveAndPublish(
                        oldCoachAccount,
                        NotificationType.APPOINTMENT_CANCELLED,
                        "You were unassigned from an appointment",
                        String.format("You were unassigned from appointment #%d at %s on %s (slot marked UNAVAILABLE).", ap.getId(), slotId, date),
                        null,
                        url,
                        deepLink
                );
            }

        } catch (Exception ex) {
            log.warn("Failed sending reassignment notifications for appointment {}: {}", ap.getId(), ex.getMessage());
        }
    }

}

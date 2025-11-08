package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.AppointmentRequest;
import com.smartquit.smartquitiot.dto.response.AppointmentResponse;
import com.smartquit.smartquitiot.dto.response.JoinTokenResponse;
import com.smartquit.smartquitiot.dto.response.RemainingBookingResponse;
import com.smartquit.smartquitiot.entity.Appointment;
import com.smartquit.smartquitiot.entity.CoachWorkSchedule;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.entity.MembershipSubscription;
import com.smartquit.smartquitiot.enums.AppointmentStatus;
import com.smartquit.smartquitiot.enums.CancelledBy;
import com.smartquit.smartquitiot.enums.CoachWorkScheduleStatus;
import com.smartquit.smartquitiot.mapper.AppointmentMapper;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.AgoraService;
import com.smartquit.smartquitiot.service.AppointmentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final FeedbackRepository feedbackRepository; // add to fields

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

}

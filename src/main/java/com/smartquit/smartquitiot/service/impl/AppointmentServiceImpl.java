package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.AppointmentRequest;
import com.smartquit.smartquitiot.dto.response.AppointmentResponse;
import com.smartquit.smartquitiot.dto.response.JoinTokenResponse;
import com.smartquit.smartquitiot.entity.Appointment;
import com.smartquit.smartquitiot.entity.CoachWorkSchedule;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.enums.AppointmentStatus;
import com.smartquit.smartquitiot.enums.CancelledBy;
import com.smartquit.smartquitiot.enums.CoachWorkScheduleStatus;
import com.smartquit.smartquitiot.mapper.AppointmentMapper;
import com.smartquit.smartquitiot.repository.AppointmentRepository;
import com.smartquit.smartquitiot.repository.CoachWorkScheduleRepository;
import com.smartquit.smartquitiot.repository.MemberRepository;
import com.smartquit.smartquitiot.service.AgoraService;
import com.smartquit.smartquitiot.service.AppointmentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final CoachWorkScheduleRepository coachWorkScheduleRepository;
    private final MemberRepository memberRepository;
    private final AppointmentMapper appointmentMapper;
    private final AgoraService agoraService;

    @Override
    @Transactional
    public AppointmentResponse bookAppointment(int accountId, AppointmentRequest request) {

        Member member = memberRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Member không tồn tại"));

        var memberId = member.getId();
        var date = request.getDate();
        var coachId = request.getCoachId();
        var slotId = request.getSlotId();

        // Kiểm tra slot đã được đặt chưa (by coachId, slotId, date, miễn không phải cancel)
        if (appointmentRepository.existsActiveByCoachSlotDate(coachId, slotId, date)) {
            throw new IllegalStateException("Slot này đã được đặt trước đó!");
        }

        // Kiểm tra slot có trong lịch làm việc không
        CoachWorkSchedule cws = coachWorkScheduleRepository
                .findByCoachIdAndDateAndSlotIdForUpdate(coachId, date, slotId)
                .orElseThrow(() -> new IllegalArgumentException("Coach không có slot này trong lịch làm việc!"));

        if (cws.getStatus() != CoachWorkScheduleStatus.AVAILABLE) {
            throw new IllegalStateException("Slot này hiện không khả dụng!");
        }

        // Cập nhật trạng thái CWS sang BOOKED
        cws.setStatus(CoachWorkScheduleStatus.BOOKED);
        coachWorkScheduleRepository.save(cws);

        // Tạo appointment mới, cập nhật appointment sang PENDING
        Appointment appointment = new Appointment();
        appointment.setAppointmentStatus(AppointmentStatus.PENDING);
        appointment.setCoach(cws.getCoach());
        appointment.setMember(member);
        appointment.setDate(cws.getDate());
        appointment.setName("Cuộc hẹn với coach " + cws.getCoach().getLastName());
        appointment.setCoachWorkSchedule(cws);
        appointmentRepository.save(appointment);

        log.info("Member(accountId={}) (memberId={}) đã đặt slot {} với coach {} vào ngày {}",
                accountId, memberId, slotId, coachId, date);

        return appointmentMapper.toResponse(appointment);
    }

    @Override
    @Transactional
    public void cancelAppointment(int appointmentId, int accountId) {
        // ktra appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment không tồn tại"));

        // ktra account
        Member member = memberRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Member không tồn tại"));

        if (appointment.getMember() == null || appointment.getMember().getId() != member.getId()) {
            throw new SecurityException("Bạn không có quyền huỷ lịch này");
        }

        // nếu đã bị huỷ rồi thì return
        if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
            log.info("Appointment {} đã ở trạng thái CANCELLED trước đó", appointmentId);
            return;
        }


        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

        CoachWorkSchedule linkedCws = appointment.getCoachWorkSchedule();
        CoachWorkSchedule cwsForTime = linkedCws;
        if (cwsForTime == null && appointment.getCoach() != null && appointment.getCoachWorkSchedule().getSlot() == null) {
            cwsForTime = coachWorkScheduleRepository.findByCoachIdAndDateAndSlotIdForUpdate(
                    appointment.getCoach().getId(), appointment.getDate(),
                    appointment.getCoachWorkSchedule().getSlot() != null ? appointment.getCoachWorkSchedule().getSlot().getId() : -1
            ).orElse(null);
        }

        if (cwsForTime == null || cwsForTime.getSlot() == null) {
            throw new IllegalStateException("Không thể xác định thông tin slot để kiểm tra thời hạn huỷ");
        }

        LocalDate apDate = appointment.getDate();
        LocalTime startTime = cwsForTime.getSlot().getStartTime();
        ZonedDateTime startZ = LocalDateTime.of(apDate, startTime).atZone(zone);
        ZonedDateTime nowZ = ZonedDateTime.now(zone);
        ZonedDateTime memberLimit = startZ.minusMinutes(5);

        if (!nowZ.isBefore(memberLimit)) {
            throw new IllegalStateException("Quá muộn để huỷ. Member phải huỷ trước 5 phút so với giờ bắt đầu slot.");
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
                // fallback: log and skip merge to avoid stale-merge issues
                log.warn("CWS not found by forUpdate for appointment {} — fallback skipped", appointmentId);
            }
        } else {
            log.warn("Appointment {} không có CoachWorkSchedule liên kết khi member cancel", appointmentId);
        }

        appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledBy(CancelledBy.MEMBER);
        appointment.setCancelledAt(LocalDateTime.now());

        appointmentRepository.save(appointment);

        log.info("Member(accountId={}) đã huỷ appointment {}", accountId, appointmentId);
    }


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
            log.info("Appointment {} đã ở trạng thái CANCELLED trước đó (coach cancel)", appointmentId);
            return;
        }

        CoachWorkSchedule linkedCws = appointment.getCoachWorkSchedule();
        if (linkedCws == null || linkedCws.getSlot() == null) {
            // Không có dữ liệu CWS/Slot -> không thể kiểm tra thời hạn huỷ an toàn
            throw new IllegalStateException("Cannot determine slot information to validate cancellation window");
        }

        // TIME CHECK: coach must cancel at least 30 minutes before slot start
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate apDate = appointment.getDate();
        LocalTime startTime = linkedCws.getSlot().getStartTime();
        ZonedDateTime startZ = LocalDateTime.of(apDate, startTime).atZone(zone);
        ZonedDateTime nowZ = ZonedDateTime.now(zone);
        ZonedDateTime coachLimit = startZ.minusMinutes(30);

        if (!nowZ.isBefore(coachLimit)) {
            throw new IllegalStateException("Too late to cancel. Coach must cancel at least 30 minutes before slot start.");
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
            log.warn("Không tìm thấy CWS bằng forUpdate cho appointment {} — dùng linkedCws để cập nhật", appointmentId);
            linkedCws.setStatus(CoachWorkScheduleStatus.UNAVAILABLE);
            coachWorkScheduleRepository.save(linkedCws);
        }

        appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledBy(CancelledBy.COACH);
        appointment.setCancelledAt(LocalDateTime.now());

        appointmentRepository.save(appointment);

        log.info("Coach(accountId={}) đã huỷ appointment {} — slot mark UNAVAILABLE", coachAccountId, appointmentId);
    }

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
                    // runtimeStatus: if appointment was cancelled, surface CANCELLED explicitly
                    String runtimeStatus;
                    if (a.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
                        runtimeStatus = "CANCELLED";
                    } else {
                        CoachWorkSchedule cws = a.getCoachWorkSchedule();
                        runtimeStatus = (cws != null && cws.getSlot() != null) ? calculateRuntimeStatus(cws) : "UNKNOWN";
                    }

                    // mapper now maps member info as well
                    return appointmentMapper.toResponseWithRuntime(a, runtimeStatus);
                })
                .toList();

        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        int from = page * size;
        if (from >= converted.size()) return List.of();
        int to = Math.min(converted.size(), from + size);
        return converted.subList(from, to);
    }

    @Override
    public List<AppointmentResponse> getAppointmentsByCoachAccountId(int coachAccountId,
                                                                     String statusFilter,
                                                                     String dateFilter,
                                                                     int page,
                                                                     int size) {

        // fetch appointments by coach's account id (controller passes accountId)
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
                    // already guaranteed a.coach.account.id == coachAccountId by query, but extra safety:
                    if (a.getCoach() == null || a.getCoach().getAccount() == null) return false;
                    if (a.getCoach().getAccount().getId() != coachAccountId) return false;
                    if (statusFinal != null && a.getAppointmentStatus() != statusFinal) return false;
                    if (dateFinal != null && (a.getDate() == null || !a.getDate().equals(dateFinal))) return false;
                    return true;
                })
                .map(a -> {
                    // runtimeStatus: CANCELLED explicit, else compute from cws
                    String runtimeStatus;
                    if (a.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
                        runtimeStatus = "CANCELLED";
                    } else {
                        CoachWorkSchedule cws = a.getCoachWorkSchedule();
                        runtimeStatus = (cws != null && cws.getSlot() != null) ? calculateRuntimeStatus(cws) : "UNKNOWN";
                    }

                    AppointmentResponse resp = appointmentMapper.toResponseWithRuntime(a, runtimeStatus);

                    // set member info so coach can see who booked
                    if (a.getMember() != null) {
                        resp.setMemberId(a.getMember().getId());
                        String mf = a.getMember().getFirstName() != null ? a.getMember().getFirstName() : "";
                        String ml = a.getMember().getLastName() != null ? a.getMember().getLastName() : "";
                        resp.setMemberName((mf + " " + ml).trim());
                    }

                    return resp;
                })
                .toList();

        // pagination
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        int from = page * size;
        if (from >= converted.size()) return List.of();
        int to = Math.min(converted.size(), from + size);
        return converted.subList(from, to);
    }


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
    @Override
    public AppointmentResponse getAppointmentDetailForPrincipal(int appointmentId, int accountId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        // accountId phải có (controller của em truyền accountId từ token)
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

        // runtime status: nếu appointment đã bị CANCELLED thì trả "CANCELLED",
        // còn không thì tính theo slot (PENDING/IN_PROGRESS/COMPLETED) hoặc UNKNOWN
        String runtimeStatus;
        if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
            runtimeStatus = "CANCELLED";
        } else {
            CoachWorkSchedule cws = appointment.getCoachWorkSchedule();
            runtimeStatus = (cws != null && cws.getSlot() != null) ? calculateRuntimeStatus(cws) : "UNKNOWN";
        }

        AppointmentResponse resp = appointmentMapper.toResponseWithRuntime(appointment, runtimeStatus);

        // bổ sung thông tin member cho response (coach cần biết ai book)
        if (appointment.getMember() != null) {
            resp.setMemberId(appointment.getMember().getId());
            String mf = appointment.getMember().getFirstName() != null ? appointment.getMember().getFirstName() : "";
            String ml = appointment.getMember().getLastName() != null ? appointment.getMember().getLastName() : "";
            resp.setMemberName((mf + " " + ml).trim());
        }

        return resp;
    }


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

        // Reject cancelled appointments
        if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Appointment is cancelled.");
        }

        // Resolve slot via CoachWorkSchedule
        CoachWorkSchedule cws = appointment.getCoachWorkSchedule();
        if (cws == null || cws.getSlot() == null) {
            throw new IllegalStateException("Cannot determine slot information for this appointment.");
        }

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate apDate = appointment.getDate();
        LocalTime start = cws.getSlot().getStartTime();
        LocalTime end = cws.getSlot().getEndTime();

        // compute window using ZonedDateTime
        ZonedDateTime windowStartZ = LocalDateTime.of(apDate, start).minusMinutes(5).atZone(zone);
        ZonedDateTime windowEndZ   = LocalDateTime.of(apDate, end).plusMinutes(5).atZone(zone);
        ZonedDateTime nowZ = ZonedDateTime.now(zone);

        if (nowZ.isBefore(windowStartZ)) {
            throw new IllegalStateException("Too early to join. Join window starts at: " + windowStartZ.toString());
        }
        if (nowZ.isAfter(windowEndZ)) {
            throw new IllegalStateException("Join window has already closed.");
        }

        // compute ttl in seconds (from now -> window end)
        long rawTtlSeconds = Duration.between(Instant.now(), windowEndZ.toInstant()).getSeconds();
        long ttlSeconds = Math.max(30, rawTtlSeconds);

        final long MAX_TTL = 86_400L; // 24 hours
        long ttlToUse = Math.min(ttlSeconds, MAX_TTL);

        int ttlInt = (int) ttlToUse;

        String channel = "appointment_" + appointmentId;

        // Generate a per-connection uid
        int randomUid = java.util.concurrent.ThreadLocalRandom.current().nextInt(100_000, 2_000_000_000);

        log.info("Tạo join token cho appointment={} bởi accountId={} (ttl={}s) -> uid={}", appointmentId, accountId, ttlInt, randomUid);

        // generate token for that uid
        String token = agoraService.generateRtcToken(channel, randomUid, ttlInt);
        long expiresAt = Instant.now().getEpochSecond() + ttlInt;

        return new JoinTokenResponse(channel, token, randomUid, expiresAt, ttlInt);
    }


}

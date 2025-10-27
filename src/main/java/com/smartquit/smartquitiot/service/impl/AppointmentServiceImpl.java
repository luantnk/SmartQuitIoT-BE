package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.AppointmentRequest;
import com.smartquit.smartquitiot.dto.response.AppointmentResponse;
import com.smartquit.smartquitiot.dto.response.JoinTokenResponse;
import com.smartquit.smartquitiot.entity.Appointment;
import com.smartquit.smartquitiot.entity.CoachWorkSchedule;
import com.smartquit.smartquitiot.entity.Member;
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

        // Resolve Account -> Member
        Member member = memberRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Member không tồn tại"));

        var memberId = member.getId();
        var date = request.getDate();
        var coachId = request.getCoachId();
        var slotId = request.getSlotId();

        // Kiểm tra slot đã được đặt chưa (by coachId, slotId, date)
        if (appointmentRepository.existsByCoachIdAndSlotIdAndDate(coachId, slotId, date)) {
            throw new IllegalStateException("Slot này đã được đặt trước đó!");
        }

        // Kiểm tra slot có trong lịch làm việc không
        CoachWorkSchedule cws = coachWorkScheduleRepository
                .findByCoachIdAndDateAndSlotId(coachId, date, slotId)
                .orElseThrow(() -> new IllegalArgumentException("Coach không có slot này trong lịch làm việc!"));

        if (cws.getStatus() != CoachWorkScheduleStatus.AVAILABLE) {
            throw new IllegalStateException("Slot này hiện không khả dụng!");
        }

        // Cập nhật trạng thái slot sang PENDING
        cws.setStatus(CoachWorkScheduleStatus.PENDING);
        coachWorkScheduleRepository.save(cws);

        // Tạo appointment mới
        Appointment appointment = new Appointment();
        appointment.setCoach(cws.getCoach());
        appointment.setMember(member);
        appointment.setSlot(cws.getSlot());
        appointment.setDate(date);
        appointment.setName("Cuộc hẹn với coach " + cws.getCoach().getLastName());

        appointmentRepository.save(appointment);

        log.info("Member(accountId={}) (memberId={}) đã đặt slot {} với coach {} vào ngày {}",
                accountId, memberId, slotId, coachId, date);

        return appointmentMapper.toResponse(appointment);
    }

    @Override
    @Transactional
    public void cancelAppointment(int appointmentId, int accountId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment không tồn tại"));

        // Resolve accountId -> member and validate ownership
        Member member = memberRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Member không tồn tại"));

        if (appointment.getMember().getId() != member.getId()) {
            throw new SecurityException("Bạn không có quyền huỷ lịch này");
        }

        // Cập nhật lại trạng thái slot sang AVAILABLE
        CoachWorkSchedule cws = coachWorkScheduleRepository
                .findByCoachIdAndDateAndSlotId(
                        appointment.getCoach().getId(),
                        appointment.getDate(),
                        appointment.getSlot().getId()
                )
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy lịch làm việc tương ứng"));

        cws.setStatus(CoachWorkScheduleStatus.AVAILABLE);
        coachWorkScheduleRepository.save(cws);

        appointmentRepository.delete(appointment);
        log.info("Member(accountId={}) đã huỷ appointment {}", accountId, appointmentId);
    }

    @Override
    @Transactional
    public List<AppointmentResponse> getAppointmentsByMemberId(int accountId) {
        // Resolve accountId -> Member
        Member member = memberRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Member không tồn tại"));

        var memberId = member.getId();

        var appointments = appointmentRepository.findAllByMemberId(memberId);

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

        return appointments.stream()
                .map(app -> {
                    var cws = coachWorkScheduleRepository.findByCoachIdAndDateAndSlotId(
                            app.getCoach().getId(),
                            app.getDate(),
                            app.getSlot().getId()
                    ).orElse(null);

                    // runtime status: compute from slot times (independent of DB status)
                    String runtimeStatus = (cws != null)
                            ? calculateRuntimeStatus(cws)
                            : "UNKNOWN";

                    // base response from mapper
                    AppointmentResponse resp = appointmentMapper.toResponseWithRuntime(app, runtimeStatus);

                    // channel / meeting url
                    String channel = "appointment_" + app.getId();
                    String meetingUrl = "/meeting/" + app.getId();
                    resp.setChannelName(channel);
                    resp.setMeetingUrl(meetingUrl);

                    // compute join window (±5 minutes) as Instants in UTC
                    LocalDate apDate = app.getDate();
                    LocalTime start = app.getSlot().getStartTime();
                    LocalTime end = app.getSlot().getEndTime();

                    ZonedDateTime windowStartZ = LocalDateTime.of(apDate, start).minusMinutes(5).atZone(zone);
                    ZonedDateTime windowEndZ   = LocalDateTime.of(apDate, end).plusMinutes(5).atZone(zone);

                    resp.setJoinWindowStart(windowStartZ.toInstant());
                    resp.setJoinWindowEnd(windowEndZ.toInstant());

                    return resp;
                })
                .toList();
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

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

        LocalDate apDate = appointment.getDate();
        LocalTime start = appointment.getSlot().getStartTime();
        LocalTime end = appointment.getSlot().getEndTime();

        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime windowStart = LocalDateTime.of(apDate, start).minusMinutes(5);
        LocalDateTime windowEnd   = LocalDateTime.of(apDate, end).plusMinutes(5);

        if (now.isBefore(windowStart)) {
            throw new IllegalStateException("Too early to join. Join window starts at: " + windowStart.atZone(zone).toString());
        }
        if (now.isAfter(windowEnd)) {
            throw new IllegalStateException("Join window has already closed.");
        }

        long rawTtl = Duration.between(now, windowEnd).getSeconds();
        long ttlSeconds = Math.max(30, rawTtl); // minimum 30 seconds

        String channel = "appointment_" + appointmentId;
        // uid = accountId as agreed
        String token = agoraService.generateRtcToken(channel, accountId, (int) ttlSeconds);
        long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;

        // if using simple constructor (no lombok)
        return new JoinTokenResponse(channel, token, accountId, expiresAt, ttlSeconds);
    }

}

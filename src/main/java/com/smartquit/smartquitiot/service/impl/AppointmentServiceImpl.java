package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.AppointmentRequest;
import com.smartquit.smartquitiot.dto.response.AppointmentResponse;
import com.smartquit.smartquitiot.entity.Appointment;
import com.smartquit.smartquitiot.entity.CoachWorkSchedule;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.enums.CoachWorkScheduleStatus;
import com.smartquit.smartquitiot.mapper.AppointmentMapper;
import com.smartquit.smartquitiot.repository.AppointmentRepository;
import com.smartquit.smartquitiot.repository.CoachWorkScheduleRepository;
import com.smartquit.smartquitiot.repository.MemberRepository;
import com.smartquit.smartquitiot.service.AppointmentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final CoachWorkScheduleRepository coachWorkScheduleRepository;
    private final MemberRepository memberRepository;
    private final AppointmentMapper appointmentMapper;

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

        return appointments.stream()
                .map(app -> {
                    var cws = coachWorkScheduleRepository.findByCoachIdAndDateAndSlotId(
                            app.getCoach().getId(),
                            app.getDate(),
                            app.getSlot().getId()
                    ).orElse(null);

                    String runtimeStatus = (cws != null)
                            ? calculateRuntimeStatus(cws)
                            : "UNKNOWN";

                    return appointmentMapper.toResponseWithRuntime(app, runtimeStatus);
                })
                .toList();
    }


    private String calculateRuntimeStatus(CoachWorkSchedule cws) {
        var today = java.time.LocalDate.now();
        var now = java.time.LocalTime.now();

        if (!cws.getDate().isEqual(today)) {
            return cws.getStatus().name();
        }

        var start = cws.getSlot().getStartTime();
        var end = cws.getSlot().getEndTime();

        if (now.isBefore(start)) return "PENDING";
        if (now.isBefore(end)) return "IN_PROGRESS";
        return "COMPLETED";
    }

}

package com.smartquit.smartquitiot.cron;

import com.smartquit.smartquitiot.entity.Appointment;
import com.smartquit.smartquitiot.enums.AppointmentStatus;
import com.smartquit.smartquitiot.repository.AppointmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentStatusCron {

    private final AppointmentRepository appointmentRepository;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");


    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void updateAppointmentStatuses() {
        try {
            LocalDate today = LocalDate.now(ZONE);

            // bỏ qua completed và cancelled
            List<AppointmentStatus> watch = List.of(AppointmentStatus.PENDING, AppointmentStatus.IN_PROGRESS);

            List<Appointment> appointments = appointmentRepository.findAllByDateAndStatusIn(today, watch);
            if (appointments == null || appointments.isEmpty()) {
                log.debug("No appointments to update for {}", today);
                return;
            }

            LocalDateTime now = LocalDateTime.now(ZONE);

            List<Appointment> toSave = appointments.stream()
                    .filter(a -> a != null && a.getCoachWorkSchedule() != null && a.getCoachWorkSchedule().getSlot() != null)
                    .peek(a -> {
                        try {
                            var slot = a.getCoachWorkSchedule().getSlot();
                            LocalTime start = slot.getStartTime();
                            LocalTime end = slot.getEndTime();
                            if (start == null || end == null) {
                                log.warn("Appointment {} has null slot times - skipping", a.getId());
                                return;
                            }

                            LocalDate apDate = a.getDate();
                            LocalDateTime startDt = LocalDateTime.of(apDate, start);
                            LocalDateTime endDt = LocalDateTime.of(apDate, end);

                            // Transition logic
                            if (now.isBefore(startDt)) {
                                // before start -> keep PENDING
                                // don't change
                            } else if (!now.isBefore(startDt) && now.isBefore(endDt)) {
                                // between start and end -> IN_PROGRESS
                                if (a.getAppointmentStatus() != AppointmentStatus.IN_PROGRESS) {
                                    a.setAppointmentStatus(AppointmentStatus.IN_PROGRESS);
                                }
                            } else if (!now.isBefore(endDt)) {
                                // after end -> COMPLETED
                                if (a.getAppointmentStatus() != AppointmentStatus.COMPLETED) {
                                    a.setAppointmentStatus(AppointmentStatus.COMPLETED);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error computing status for appointment {}: {}", a.getId(), e.getMessage(), e);
                        }
                    })
                    // filter only those we changed (to avoid unnecessary saves)
                    .filter(a -> a.getAppointmentStatus() == AppointmentStatus.IN_PROGRESS || a.getAppointmentStatus() == AppointmentStatus.COMPLETED)
                    .collect(Collectors.toList());

            if (!toSave.isEmpty()) {
                appointmentRepository.saveAll(toSave);
                log.info("Auto-updated {} appointments for {}", toSave.size(), today);
            } else {
                log.debug("No appointment status changes for {}", today);
            }
        } catch (Exception ex) {
            log.error("AppointmentStatusCron failed: {}", ex.getMessage(), ex);
        }
    }
}

package com.smartquit.smartquitiot.cron;

import com.smartquit.smartquitiot.entity.Appointment;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.enums.NotificationType;
import com.smartquit.smartquitiot.enums.AppointmentStatus;
import com.smartquit.smartquitiot.repository.AppointmentRepository;
import com.smartquit.smartquitiot.repository.NotificationRepository;
import com.smartquit.smartquitiot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
//@ConditionalOnProperty(prefix = "scheduler.reminder", name = "enabled", havingValue = "true")
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    // reminder offset in minutes
    private static final int REMINDER_MINUTES = 10;

    @Scheduled(fixedDelayString = "${scheduler.reminder.ms:300000}", initialDelayString = "${scheduler.reminder.initialDelayMs:10000}")
    @Transactional
    public void remindUpcomingAppointments() {
        try {
            ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
            ZonedDateTime now = ZonedDateTime.now(zone);
            ZonedDateTime target = now.plusMinutes(REMINDER_MINUTES);

            LocalDate date = target.toLocalDate();
            LocalTime from = target.toLocalTime().minusSeconds(60);
            LocalTime to = target.toLocalTime().plusSeconds(60);

            log.debug("ReminderScheduler: scanning appointments for date={} time between {} and {}", date, from, to);

            List<Appointment> upcoming = appointmentRepository.findAppointmentsForReminder(date, from, to, AppointmentStatus.PENDING);

            for (Appointment a : upcoming) {
                try {
                    if (a.getCoach() == null || a.getCoach().getAccount() == null) {
                        log.warn("Appointment {} missing coach/account -> skip reminder", a.getId());
                        continue;
                    }

                    Account coachAccount = a.getCoach().getAccount();
                    int coachAccountId = coachAccount.getId();
                    String deepLink = "smartquit://appointment/" + a.getId();

                    boolean already = notificationRepository
                            .existsByAccount_IdAndNotificationTypeAndDeepLinkAndIsDeletedFalse(
                                    coachAccountId, NotificationType.APPOINTMENT_REMINDER, deepLink
                            );
                    if (already) {
                        log.debug("Reminder already sent for appointment {} -> skip", a.getId());
                        continue;
                    }

                    LocalDate apDate = a.getCoachWorkSchedule().getDate();
                    LocalTime startTime = a.getCoachWorkSchedule().getSlot().getStartTime();
                    ZonedDateTime startZ = LocalDateTime.of(apDate, startTime).atZone(zone);
                    ZonedDateTime nowZ = ZonedDateTime.now(zone);

                    long minutesUntilStart = Duration.between(nowZ, startZ).toMinutes();

                    String title = "Upcoming appointment in " + minutesUntilStart + " minutes";
                    String content = String.format("You have an appointment (id=%d) scheduled at %s. Please be ready.",
                            a.getId(),
                            startTime.toString()
                    );

                    notificationService.saveAndPublish(
                            coachAccount,
                            NotificationType.APPOINTMENT_REMINDER,
                            title,
                            content,
                            null,
                            "appointments/" + a.getId(),
                            deepLink
                    );

                    log.info("Sent reminder for appointment {} to coachAccount={}", a.getId(), coachAccountId);
                } catch (Exception e) {
                    log.error("Failed to send reminder for appointment {}: {}", a.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception ex) {
            log.error("AppointmentReminderScheduler failed: {}", ex.getMessage(), ex);
        }
    }
}

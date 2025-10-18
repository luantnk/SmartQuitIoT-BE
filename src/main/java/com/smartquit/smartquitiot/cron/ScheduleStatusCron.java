package com.smartquit.smartquitiot.cron;

import com.smartquit.smartquitiot.entity.CoachWorkSchedule;
import com.smartquit.smartquitiot.enums.CoachWorkScheduleStatus;
import com.smartquit.smartquitiot.repository.CoachWorkScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleStatusCron {

    private final CoachWorkScheduleRepository coachWorkScheduleRepository;

    @Scheduled(fixedRate = 300000) // 5 phút
    @Transactional
    public void updateScheduleStatuses() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<CoachWorkSchedule> schedules =
                coachWorkScheduleRepository.findAllByDateAndStatusIn(
                        today,
                        List.of(
                                CoachWorkScheduleStatus.PENDING,
                                CoachWorkScheduleStatus.IN_PROGRESS
                        )
                );

        for (CoachWorkSchedule cws : schedules) {
            if (cws.getStatus() == CoachWorkScheduleStatus.PENDING &&
                    cws.getSlot().getStartTime().isBefore(now)) {
                cws.setStatus(CoachWorkScheduleStatus.IN_PROGRESS);
            } else if (cws.getStatus() == CoachWorkScheduleStatus.IN_PROGRESS &&
                    cws.getSlot().getEndTime().isBefore(now)) {
                cws.setStatus(CoachWorkScheduleStatus.COMPLETED);
            }
        }

        coachWorkScheduleRepository.saveAll(schedules);
        log.info("Auto updated {} schedules for {}", schedules.size(), today);
    }
}

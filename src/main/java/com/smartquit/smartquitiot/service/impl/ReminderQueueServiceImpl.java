package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.NotificationType;
import com.smartquit.smartquitiot.enums.PhaseEnum;
import com.smartquit.smartquitiot.enums.ReminderQueueStatus;
import com.smartquit.smartquitiot.enums.ReminderType;
import com.smartquit.smartquitiot.repository.DiaryRecordRepository;
import com.smartquit.smartquitiot.repository.QuitPlanRepository;
import com.smartquit.smartquitiot.repository.ReminderQueueRepository;
import com.smartquit.smartquitiot.repository.ReminderTemplateRepository;
import com.smartquit.smartquitiot.service.NotificationService;
import com.smartquit.smartquitiot.service.ReminderQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderQueueServiceImpl implements ReminderQueueService {

    private final ReminderTemplateRepository reminderTemplateRepository;
    private final ReminderQueueRepository reminderQueueRepository;
    private final DiaryRecordRepository diaryRecordRepository;
    private final NotificationService notificationService;
    private final QuitPlanRepository quitPlanRepository;

    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void dispatchReminders() {
        LocalDateTime now = LocalDateTime.now();

        log.info("REMINDER SCHEDULER RUNNING AT");

        List<ReminderQueue> dueReminders =
                reminderQueueRepository.findByStatusAndScheduledAtBefore(
                        ReminderQueueStatus.PENDING,
                        now
                );

        if (dueReminders.isEmpty()) {
            log.info("No reminders to send.");
            return;
        }

        log.info("Found {} reminder(s) due for sending…", dueReminders.size());

        for (ReminderQueue rq : dueReminders) {
            try {
                LocalDate today = now.toLocalDate();

                // Nếu là reminder của hôm nay nhưng đã trễ quá 1h bỏ luôn
                if (rq.getScheduledAt().toLocalDate().isEqual(today)
                        && rq.getScheduledAt().isBefore(now.minusHours(1))) {

                    rq.setStatus(ReminderQueueStatus.CANCELLED);
                    reminderQueueRepository.save(rq);
                    continue;
                }

                Member member = rq.getPhaseDetail().getPhase().getQuitPlan().getMember();

                notificationService.saveAndPublish(
                        member,
                        NotificationType.REMINDER,
                        "SmartQuit Reminder",
                        rq.getContent(),
                        null,
                        null,
                        "smartquit://reminder"
                );

                rq.setStatus(ReminderQueueStatus.SENT);
                reminderQueueRepository.save(rq);

            } catch (Exception e) {
                rq.setStatus(ReminderQueueStatus.CANCELLED);
                reminderQueueRepository.save(rq);
            }
        }

    }

    @Override
    @Transactional
    public void createDailyRemindersForPhase(Phase phase, List<PhaseDetail> details) {

        Member member = phase.getQuitPlan().getMember();


        LocalTime morningTime = member.getMorningReminderTime() != null
                ? member.getMorningReminderTime()
                : LocalTime.of(7, 0);

        LocalTime quietStart = member.getQuietStart();
        LocalTime quietEnd = member.getQuietEnd();

        String timeZone = member.getTimeZone() != null
                ? member.getTimeZone()
                : "Asia/Ho_Chi_Minh";

        ZoneId zone = ZoneId.of(timeZone);

        //Templates cho MORNING
        List<ReminderTemplate> morningTemplates =
                reminderTemplateRepository.findByReminderTypeAndPhaseEnum(ReminderType.MORNING,
                        toPhaseEnum(phase));

        // Nếu không có template morning thì thôi khỏi gen MORNING
        boolean hasMorningTemplate = !morningTemplates.isEmpty();

        // Templates cho BEHAVIOR – sẽ lọc theo trigger sau
        List<ReminderTemplate> behaviorTemplates =
                reminderTemplateRepository.findByReminderType(ReminderType.BEHAVIOR);

        // Triggers chính của member (ví dụ STRESS, COFFEE,…)
        List<String> memberTriggers = getFrequentlyTriggeredReminders(member.getId());

        for (PhaseDetail pd : details) {
            LocalDate phaseDate = pd.getDate();
            //MORNING
            if (hasMorningTemplate) {
                ReminderTemplate morningChosen = pickRandom(morningTemplates);

                LocalDateTime morningCandidate =
                        LocalDateTime.of(phaseDate, morningTime);

                LocalDateTime morningScheduleAt =
                        adjustForQuietTime(morningCandidate, quietStart, quietEnd);

                ReminderQueue morningQueue = new ReminderQueue();
                morningQueue.setPhaseDetail(pd);
                morningQueue.setReminderTemplate(morningChosen);
                morningQueue.setContent(morningChosen.getContent());
                morningQueue.setScheduledAt(morningScheduleAt);
                morningQueue.setStatus(ReminderQueueStatus.PENDING);

                reminderQueueRepository.save(morningQueue);
            }

            // BEHAVIOR
            if (!memberTriggers.isEmpty() && !behaviorTemplates.isEmpty()) {
                // random 1 trigger trong số trigger của member
                String triggerForThisDay = pickRandom(memberTriggers);
                    log.info("triggerForThisDay {}", triggerForThisDay);
                // lọc template behavior nào có triggerCode phù hợp
                List<ReminderTemplate> matchedBehaviorTemplates = behaviorTemplates.stream()
                        .filter(t -> triggerForThisDay.equalsIgnoreCase(t.getTriggerCode()))
                        .toList();

                if (!matchedBehaviorTemplates.isEmpty()) {
                    ReminderTemplate behaviorChosen = pickRandom(matchedBehaviorTemplates);

                    // ví dụ behavior gửi sau MORNING 3 tiếng
                    LocalTime behaviorBaseTime = morningTime.plusHours(3);
                    if (behaviorBaseTime.isAfter(LocalTime.of(22, 0))) {
                        // nếu cộng 3 tiếng lố quá tối thì cho lại 15h
                        behaviorBaseTime = LocalTime.of(15, 0);
                    }

                    LocalDateTime behaviorCandidate =
                            LocalDateTime.of(phaseDate, behaviorBaseTime);

                    LocalDateTime behaviorScheduleAt =
                            adjustForQuietTime(behaviorCandidate, quietStart, quietEnd);

                    ReminderQueue behaviorQueue = new ReminderQueue();
                    behaviorQueue.setPhaseDetail(pd);
                    behaviorQueue.setReminderTemplate(behaviorChosen);
                    behaviorQueue.setContent(behaviorChosen.getContent());
                    behaviorQueue.setScheduledAt(behaviorScheduleAt);
                    behaviorQueue.setStatus(ReminderQueueStatus.PENDING);

                    reminderQueueRepository.save(behaviorQueue);
                }
            }
        }
    }

    //from Log Diary Record
    private List<String> getFrequentlyTriggeredReminders(Integer memberId) {

        QuitPlan plan = quitPlanRepository.findByMember_IdAndIsActiveTrue(memberId);
        if (plan == null) {
            throw new RuntimeException("No active Quit Plan found when using getFrequentlyTriggeredReminders");
        }

        List<DiaryRecord> diaryRecords =
                diaryRecordRepository.findByMemberIdOrderByDateDesc(memberId);

        // CASE 1: Diary rỗng
        if (diaryRecords.isEmpty()) {
            log.info("[REMINDER] No diary records. Fallback to FormMetric.triggered or default.");

            FormMetric fm = plan.getFormMetric();
            if (fm != null && fm.getTriggered() != null && !fm.getTriggered().isEmpty()) {
                return fm.getTriggered().stream()
                        .map(this::normalizeTrigger)
                        .collect(Collectors.toList());
            }

            return getDefaultTriggers();
        }

        //CASE 2: Diary có nhưng triggers rỗng
        Map<String, Long> triggerCount = diaryRecords.stream()
                .filter(r -> r.getTriggers() != null && !r.getTriggers().isEmpty())
                .flatMap(r -> r.getTriggers().stream())
                .map(this::normalizeTrigger)
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        if (triggerCount.isEmpty()) {
            log.info("[REMINDER] Diary exists but no triggers. Checking FormMetric…");

            FormMetric fm = plan.getFormMetric();
            if (fm != null && fm.getTriggered() != null && !fm.getTriggered().isEmpty()) {
                return fm.getTriggered().stream()
                        .map(this::normalizeTrigger)
                        .collect(Collectors.toList());
            }

            return getDefaultTriggers();
        }

        // CASE 3: Có trigger thống kê được
        return triggerCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))  // DESC
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }


    private List<String> getDefaultTriggers() {
        return List.of(
                "MORNING",
                "AFTER_MEAL",
                "GAMING",
                "PARTY",
                "COFFEE",
                "STRESS",
                "BOREDOM",
                "DRIVING",
                "SADNESS",
                "WORK"
        );
    }

    private String normalizeTrigger(String raw) {
        if (raw == null) return null;
        return raw.trim()
                .toUpperCase()
                .replace(" ", "_");
    }


    // Random generic cho List<T>
    private <T> T pickRandom(List<T> list) {
        int idx = ThreadLocalRandom.current().nextInt(list.size());
        return list.get(idx);
    }

    private LocalDateTime adjustForQuietTime(LocalDateTime candidate,
                                             LocalTime quietStart,
                                             LocalTime quietEnd) {
        if (quietStart == null || quietEnd == null) {
            return candidate;
        }

        LocalTime t = candidate.toLocalTime();

        // case 1: quiet không qua đêm (vd: 21:00–23:00)
        if (quietStart.isBefore(quietEnd)) {
            if (!t.isBefore(quietStart) && !t.isAfter(quietEnd)) {
                // nếu đang rơi trong quiet dời tới quietEnd + 5'
                return LocalDateTime.of(candidate.toLocalDate(), quietEnd).plusMinutes(5);
            } else {
                return candidate;
            }
        }

        // case 2: quiet qua đêm (vd: 22:00–06:00)
        boolean inQuiet =
                (!t.isBefore(quietStart) && t.isBefore(LocalTime.MIDNIGHT)) ||
                        (t.isAfter(LocalTime.MIDNIGHT.minusNanos(1)) && !t.isAfter(quietEnd));

        if (!inQuiet) {
            return candidate;
        }

        // Nếu đang rơi trong quiet buổi tối (sau quietStart)
        if (!t.isBefore(quietStart)) {
            // dời sang ngày hôm sau quietEnd + 5'
            return LocalDateTime.of(candidate.toLocalDate().plusDays(1), quietEnd)
                    .plusMinutes(5);
        } else {
            // đang trong quiet sáng sớm (trước quietEnd)  dời tới quietEnd + 5' cùng ngày
            return LocalDateTime.of(candidate.toLocalDate(), quietEnd)
                    .plusMinutes(5);
        }
    }

    private PhaseEnum toPhaseEnum(Phase phase) {
        if (phase == null || phase.getName() == null) {
            throw new IllegalArgumentException("Phase or phase name is null");
        }

        String normalized = phase.getName()
                .trim()
                .toUpperCase()
                .replace(" ", "_"); // phòng DB ghi Peak Craving → PEAK_CRAVING

        try {
            return PhaseEnum.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid phase name: " + phase.getName());
        }
    }

}

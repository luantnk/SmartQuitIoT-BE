package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.NotificationDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.NotificationType;
import com.smartquit.smartquitiot.enums.PhaseStatus;
import com.smartquit.smartquitiot.enums.QuitPlanStatus;
import com.smartquit.smartquitiot.mapper.NotificationMapper;
import com.smartquit.smartquitiot.repository.MemberRepository;
import com.smartquit.smartquitiot.repository.NotificationRepository;
import com.smartquit.smartquitiot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private static final String TOPIC_NOTIFICATIONS_FMT = "/topic/notifications/%d";
    private static final String DEFAULT_PHASE_ICON = "/static/icons/phase.png";
    private static final String DEFAULT_QUIT_PLAN_ICON = "/static/icons/quitplan.png";

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public NotificationDTO saveAndPublish(
            Member member,
            NotificationType type,
            String title,
            String content,
            String icon,
            String url,
            String deepLink
    ) {
        Notification n = new Notification();
        n.setMember(member);
        n.setNotificationType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setIcon(icon);
        n.setUrl(url);       // web route
        n.setDeepLink(deepLink); // mobile deep link

        Notification saved = notificationRepository.save(n);
        NotificationDTO dto = notificationMapper.mapToNotificationDTO(saved);

        String topic = String.format(TOPIC_NOTIFICATIONS_FMT, member.getId());
        messagingTemplate.convertAndSend(topic, dto);

        return dto;
    }

    @Override
    public NotificationDTO saveAndSendAchievementNoti(Member member, Achievement achievement) {
        log.info("saveAndSendAchievementNotification for member {}", member.getId());

        String title = "New achievement unlocked: " + achievement.getName();
        String content = (achievement.getDescription() != null && !achievement.getDescription().isBlank())
                ? achievement.getDescription()
                : ("You’ve unlocked: " + achievement.getName());

        String url = "notifications/achievements/" + achievement.getId();
        String deepLink = "smartquit://achievement/" + achievement.getId();

        return saveAndPublish(
                member,
                NotificationType.ACHIEVEMENT,
                title,
                content,
                achievement.getIcon(),
                url,
                deepLink
        );
    }

    @Override
    public NotificationDTO saveAndSendPhaseNoti(
            Member member,
            Phase phase,
            PhaseStatus toStatus,
            int newMissionsCount
    ) {
        String title = "Phase " + phase.getName() + " " + toStatus.name();
        String content = switch (toStatus) {
            case COMPLETED -> "You have completed the phase: " + phase.getName()
                    + (newMissionsCount > 0 ? (". " + newMissionsCount + " new mission(s) generated.") : ".");
            case IN_PROGRESS -> "Phase " + phase.getName() + " has started. Keep going!";
            case FAILED -> "Phase " + phase.getName() + " did not meet the conditions. Please review your targets.";
            default -> "Phase status updated: " + toStatus.name();
        };

        String url = "notifications/phases/" + phase.getId();
        String deepLink = "smartquit://phase/" + phase.getId();

        return saveAndPublish(
                member,
                NotificationType.PHASE,
                title,
                content,
                DEFAULT_PHASE_ICON,
                url,
                deepLink
        );
    }

    @Override
    public NotificationDTO saveAndSendQuitPlanNoti(Member member, QuitPlan plan, QuitPlanStatus toStatus) {
        String title = "Quit Plan " + toStatus.name();
        String content = switch (toStatus) {
            case IN_PROGRESS -> "Your quit plan has started: " + plan.getName();
            case COMPLETED -> "Congratulations! You have completed your quit plan: " + plan.getName();
            case CANCELED -> "Your quit plan \"" + plan.getName() + "\" was not completed. Adjust and try again.";
            default -> "Quit plan status updated: " + toStatus.name();
        };

        String url = "notifications/quit-plans/" + plan.getId();
        String deepLink = "smartquit://quit-plan/" + plan.getId();

        return saveAndPublish(
                member,
                NotificationType.QUIT_PLAN,
                title,
                content,
                DEFAULT_QUIT_PLAN_ICON,
                url,
                deepLink
        );
    }


}

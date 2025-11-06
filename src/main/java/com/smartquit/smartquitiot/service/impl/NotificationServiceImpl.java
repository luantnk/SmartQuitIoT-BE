package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.GetAllNotificationsRequest;
import com.smartquit.smartquitiot.dto.response.NotificationDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.NotificationType;
import com.smartquit.smartquitiot.enums.PhaseStatus;
import com.smartquit.smartquitiot.enums.QuitPlanStatus;
import com.smartquit.smartquitiot.mapper.NotificationMapper;
import com.smartquit.smartquitiot.repository.MemberRepository;
import com.smartquit.smartquitiot.repository.NotificationRepository;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private static final String TOPIC_NOTIFICATIONS_FMT = "/topic/notifications/%d";

    @Value("${default.icon.notification}")
    private String DEFAULT_PHASE_ICON;
    @Value("${default.icon.notification}")
    private String DEFAULT_QUIT_PLAN_ICON;

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final AccountService accountService;

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

    @Override
    public Page<NotificationDTO> getAll(GetAllNotificationsRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );


        Integer memberId = accountService.getAuthenticatedAccount().getMember().getId();

        Specification<Notification> spec = Specification.allOf(
                (root, query, cb) -> cb.equal(root.get("member").get("id"), memberId),
                (root, q, cb) -> cb.isFalse(root.get("isDeleted")),
                hasRead(request.getIsRead()),
                hasType(request.getType())
        );

        Page<Notification> page = notificationRepository.findAll(spec, pageable);
        return page.map(notificationMapper::mapToNotificationDTO);
    }

    @Transactional
    @Override
    public void markReadById(int notificationId) {
        int memberId = accountService.getAuthenticatedAccount().getMember().getId();
        int updated = notificationRepository.markOneRead(notificationId, memberId);
        if (updated == 0) {
            throw new IllegalArgumentException("Notification not found or already deleted");
        }
    }

    @Transactional
    @Override
    public int markAllRead() {
        int memberId = accountService.getAuthenticatedAccount().getMember().getId();
        return notificationRepository.markAllRead(memberId);
    }

    @Transactional
    @Override
    public int deleteAll() {
        int memberId = accountService.getAuthenticatedAccount().getMember().getId();
        return notificationRepository.deleteAll(memberId);
    }

    @Transactional
    @Override
    public void deleteOne(int notificationId) {
        int memberId = accountService.getAuthenticatedAccount().getMember().getId();
        int updated = notificationRepository.deleteOne(notificationId, memberId);
        if (updated == 0) {
            throw new IllegalArgumentException("Notification not found or already deleted");
        }
    }

    private Specification<Notification> hasRead(Boolean isRead) {
        if (isRead == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("isRead"), isRead);
    }

    private Specification<Notification> hasType(NotificationType type) {
        if (type == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("notificationType"), type);
    }
}

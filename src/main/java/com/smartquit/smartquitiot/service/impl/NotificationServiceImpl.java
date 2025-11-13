package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.GetAllNotificationsRequest;
import com.smartquit.smartquitiot.dto.response.NotificationDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.NotificationType;
import com.smartquit.smartquitiot.enums.PhaseStatus;
import com.smartquit.smartquitiot.enums.QuitPlanStatus;
import com.smartquit.smartquitiot.mapper.NotificationMapper;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
            Account account,
            NotificationType type,
            String title,
            String content,
            String icon,
            String url,
            String deepLink
    ) {
        Notification n = new Notification();
        n.setAccount(account);
        n.setNotificationType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setIcon(icon != null ? icon : DEFAULT_QUIT_PLAN_ICON);
        n.setUrl(url != null ? url : "notifications/default");       // web route
        n.setDeepLink(deepLink != null ? deepLink : "smartquit://default"); // mobile deep link

        Notification saved = notificationRepository.save(n);
        NotificationDTO dto = notificationMapper.mapToNotificationDTO(saved);

        String topic = String.format(TOPIC_NOTIFICATIONS_FMT, account.getId());
        // Phần này để đảm bảo Transaction kịp commit nha Hải Linh
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend(topic, dto);
                }
            });
        } else {
            // fallback (no transaction active)
            messagingTemplate.convertAndSend(topic, dto);
        }

        return dto;
    }


    @Override
    public NotificationDTO saveAndSendAchievementNoti(Account account, Achievement achievement) {
        log.info("saveAndSendAchievementNotification for account {}", account.getId());

        String title = "New achievement unlocked: " + achievement.getName();
        String content = (achievement.getDescription() != null && !achievement.getDescription().isBlank())
                ? achievement.getDescription()
                : ("You’ve unlocked: " + achievement.getName());

        String url = "notifications/achievements/" + achievement.getId();
        String deepLink = "smartquit://achievement/" + achievement.getId();

        return saveAndPublish(
                account,
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
            Account account,
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
                account,
                NotificationType.PHASE,
                title,
                content,
                DEFAULT_PHASE_ICON,
                url,
                deepLink
        );
    }

    @Override
    public NotificationDTO saveAndSendQuitPlanNoti(Account account, QuitPlan plan, QuitPlanStatus toStatus) {
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
                account,
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


        Integer accountId = accountService.getAuthenticatedAccount().getId();

        Specification<Notification> spec = Specification.allOf(
                (root, query, cb) -> cb.equal(root.get("account").get("id"), accountId),
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
        int accountId = accountService.getAuthenticatedAccount().getId();
        int updated = notificationRepository.markOneRead(notificationId, accountId);
        if (updated == 0) {
            throw new IllegalArgumentException("Notification not found or already deleted");
        }
    }

    @Transactional
    @Override
    public int markAllRead() {
        int accountId = accountService.getAuthenticatedAccount().getId();
        return notificationRepository.markAllRead(accountId);
    }

    @Transactional
    @Override
    public int deleteAll() {
        int accountId = accountService.getAuthenticatedAccount().getId();
        return notificationRepository.deleteAll(accountId);
    }

    @Transactional
    @Override
    public void deleteOne(int notificationId) {
        int accountId = accountService.getAuthenticatedAccount().getId();
        int updated = notificationRepository.deleteOne(notificationId, accountId);
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

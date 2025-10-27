package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.NotificationDTO;
import com.smartquit.smartquitiot.entity.Achievement;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.entity.Notification;
import com.smartquit.smartquitiot.enums.NotificationType;
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
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public NotificationDTO saveAndSendAchievementNoti(Member member, Achievement a) {
        log.info("saveAndSendAchievementNoti for member " + member.getId());
        Notification n = new Notification();
        n.setMember(member);
        n.setNotificationType(NotificationType.ACHIEVEMENT);
        n.setTitle("New achievement unlocked" + a.getName());
        n.setContent(a.getDescription() != null ? a.getDescription()
                : ("You’ve unlocked: " + a.getName()));
        n.setIcon(a.getIcon());
        n.setUrl("notifications/achievements/" + a.getId());               // web route
        n.setDeepLink("smartquit://achievement/" + a.getId()); // mobile deep link
        NotificationDTO notificationDTO = notificationMapper
                .mapToNotificationDTO(notificationRepository.save(n));
        messagingTemplate.convertAndSend("/topic/notifications/" + member.getId(), notificationDTO);
        return notificationDTO;
    }
}

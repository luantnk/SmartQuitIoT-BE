package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.GetAllNotificationsRequest;
import com.smartquit.smartquitiot.dto.response.NotificationDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.NotificationType;
import com.smartquit.smartquitiot.enums.PhaseStatus;
import com.smartquit.smartquitiot.enums.QuitPlanStatus;
import org.springframework.data.domain.Page;

public interface NotificationService {
    NotificationDTO saveAndSendAchievementNoti(Account account, Achievement a);
    NotificationDTO saveAndSendPhaseNoti(
            Account account,
            Phase phase,
            PhaseStatus toStatus,
            int newMissionsCount
    );
    NotificationDTO saveAndSendQuitPlanNoti(
            Account account,
            QuitPlan plan,
            QuitPlanStatus toStatus
    );
    NotificationDTO saveAndPublish(
            Account account,
            NotificationType type,
            String title,
            String content,
            String icon,
            String url,
            String deepLink
    );
    Page<NotificationDTO> getAll(GetAllNotificationsRequest request);
    void markReadById(int notificationId);
    int markAllRead();
    int deleteAll();
    void deleteOne(int notificationId);
}

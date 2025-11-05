package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.response.NotificationDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.NotificationType;
import com.smartquit.smartquitiot.enums.PhaseStatus;
import com.smartquit.smartquitiot.enums.QuitPlanStatus;

public interface NotificationService {
    NotificationDTO saveAndSendAchievementNoti(Member member, Achievement a);
    NotificationDTO saveAndSendPhaseNoti(
            Member member,
            Phase phase,
            PhaseStatus toStatus,
            int newMissionsCount
    );
    NotificationDTO saveAndSendQuitPlanNoti(
            Member member,
            QuitPlan plan,
            QuitPlanStatus toStatus
    );
    NotificationDTO saveAndPublish(
            Member member,
            NotificationType type,
            String title,
            String content,
            String icon,
            String url,
            String deepLink
    );
}

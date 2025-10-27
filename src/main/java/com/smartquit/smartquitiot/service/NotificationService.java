package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.response.NotificationDTO;
import com.smartquit.smartquitiot.entity.Achievement;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.entity.Notification;

public interface NotificationService {
    NotificationDTO saveAndSendAchievementNoti(Member member, Achievement a);
}

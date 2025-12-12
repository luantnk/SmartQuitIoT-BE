package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.NotificationDTO;
import com.smartquit.smartquitiot.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationDTO mapToNotificationDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setContent(notification.getContent());
        dto.setRead(notification.isRead());
        dto.setDeleted(notification.isDeleted());
        dto.setUrl(notification.getUrl());
        dto.setDeepLink(notification.getDeepLink());
        dto.setType(notification.getNotificationType().name());
        dto.setIcon(notification.getIcon());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}

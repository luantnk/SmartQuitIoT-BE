package com.smartquit.smartquitiot.dto.request;

import com.smartquit.smartquitiot.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetAllNotificationsRequest {
    Boolean isRead;
    NotificationType type;
    int page;
    int size;
}

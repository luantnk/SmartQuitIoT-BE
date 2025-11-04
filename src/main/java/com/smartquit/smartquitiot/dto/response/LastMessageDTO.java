package com.smartquit.smartquitiot.dto.response;

import com.smartquit.smartquitiot.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LastMessageDTO {
    private int id;
    private int senderId;
    private MessageType messageType;
    private String content;
    private LocalDateTime sentAt;
}
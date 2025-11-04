package com.smartquit.smartquitiot.dto.request;

import com.smartquit.smartquitiot.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsMessageRequest {
    private Integer conversationId;
    private Integer targetUserId;
    private MessageType messageType;
    private String content;
    private List<String> attachments;
    private String clientMessageId;
}

package com.smartquit.smartquitiot.dto.request;

import com.smartquit.smartquitiot.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MessageCreateRequest {
    private Integer conversationId;

    // target user (coach or member) when creating a new direct conversation
    private Integer targetUserId;

    @NotNull
    private MessageType messageType;

    @NotBlank
    private String content;

    private List<String> attachments;

    //  client-side idempotency key (tránh trùng)
    private String clientMessageId;
}

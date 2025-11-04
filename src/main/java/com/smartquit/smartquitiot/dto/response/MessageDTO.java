package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartquit.smartquitiot.enums.MessageType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageDTO {
    private int id;
    private int conversationId;
    private int senderId;
    private MessageType messageType;
    private String content;
    private List<String> attachments;
    private LocalDateTime sentAt;
    private boolean isDeleted;
    private String clientMessageId;
}

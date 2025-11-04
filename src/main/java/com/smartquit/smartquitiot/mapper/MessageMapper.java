package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.MessageDTO;
import com.smartquit.smartquitiot.entity.Message;

import java.util.stream.Collectors;

public class MessageMapper {
    public static MessageDTO toResponse(Message m, String clientMessageId) {
        return MessageDTO.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .senderId(m.getSender().getId())
                .messageType(m.getMessageType())
                .content(m.getContent())
                .attachments(m.getAttachments() == null ? null :
                        m.getAttachments().stream().map(a -> a.getAttachmentUrl()).collect(Collectors.toList()))
                .sentAt(m.getSentAt())
                .isDeleted(m.isDeleted())
                .clientMessageId(clientMessageId)
                .build();
    }
}

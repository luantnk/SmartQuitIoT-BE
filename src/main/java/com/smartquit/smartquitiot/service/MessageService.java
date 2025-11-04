package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.MessageCreateRequest;
import com.smartquit.smartquitiot.dto.response.MessageDTO;

import java.util.List;

public interface MessageService {
    MessageDTO sendMessage(MessageCreateRequest req);
    List<MessageDTO> getMessages(int conversationId, Integer beforeId, int limit);
}

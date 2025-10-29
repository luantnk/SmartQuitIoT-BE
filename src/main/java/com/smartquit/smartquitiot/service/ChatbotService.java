package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.ChatbotPayload;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface ChatbotService {
    List<Message> getChatbotMessagesByConversationId(Integer conversationId);

    AssistantMessage personalizedChat(ChatbotPayload payload);
}

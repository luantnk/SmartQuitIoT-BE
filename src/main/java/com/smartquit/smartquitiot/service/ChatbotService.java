package com.smartquit.smartquitiot.service;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface ChatbotService {
    List<Message> getChatbotMessagesByConversationId(Integer conversationId);
}

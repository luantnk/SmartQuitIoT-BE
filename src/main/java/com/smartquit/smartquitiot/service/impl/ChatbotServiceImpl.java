package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private final ChatMemoryRepository chatMemoryRepository;

    @Override
    public List<Message> getChatbotMessagesByConversationId(Integer conversationId) {
        return List.of();
    }
}

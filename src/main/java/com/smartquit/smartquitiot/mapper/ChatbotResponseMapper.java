package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.ChatbotResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;

@Component
public class ChatbotResponseMapper {

    public ChatbotResponse toChatbotResponse(AssistantMessage message) {
        if(message == null) return null;
        ChatbotResponse response = new ChatbotResponse();
        response.setMessageType(message.getMessageType().name());
        response.setToolCalls(message.getToolCalls());
        response.setMedia(message.getMedia());
        response.setText(message.getText());
        return response;
    }

}

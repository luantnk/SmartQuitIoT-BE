package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.ChatbotPayload;
import com.smartquit.smartquitiot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping
    public ResponseEntity<?> chatBot(@RequestBody ChatbotPayload chatbotPayload){
        return ResponseEntity.ok(chatbotService.personalizedChat(chatbotPayload));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<?> getChatbotMessagesByConversationId(@PathVariable Integer conversationId){
        return ResponseEntity.ok(chatbotService.getChatbotMessagesByConversationId(conversationId));
    }
}

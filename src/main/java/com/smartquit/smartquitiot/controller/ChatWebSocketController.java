package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.MessageCreateRequest;
import com.smartquit.smartquitiot.dto.request.WsMessageRequest;
import com.smartquit.smartquitiot.dto.response.MessageDTO;
import com.smartquit.smartquitiot.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP controller: client gửi tới /app/conversations/messages
 * Controller chỉ map payload -> MessageCreateRequest -> gọi MessageService.sendMessage(...)
 * Service chịu trách nhiệm persist + broadcast (afterCommit).
 */
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate; // chỉ để gửi lỗi về user nếu cần

    @MessageMapping("/conversations/messages")
    public void onMessage(@Payload WsMessageRequest payload, Principal principal) {
        try {
            if (principal == null) {
                log.warn("STOMP message received without principal");
                return;
            }

            MessageCreateRequest req = new MessageCreateRequest();
            req.setConversationId(payload.getConversationId());
            req.setTargetUserId(payload.getTargetUserId());
            req.setMessageType(payload.getMessageType());
            req.setContent(payload.getContent());
            req.setAttachments(payload.getAttachments());
            req.setClientMessageId(payload.getClientMessageId());

            // Service sẽ persist + broadcast (sau commit)
            MessageDTO saved = messageService.sendMessage(req);
            log.debug("WS: message persisted id={} conv={}", saved != null ? saved.getId() : null, saved != null ? saved.getConversationId() : null);

            // NOTE: KHÔNG convertAndSend ở đây để tránh double-send,
            // vì MessageServiceImpl đã broadcast sau commit.
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid WS payload: {}", ex.getMessage());
            sendErrorToUser(principal, "Bad request: " + ex.getMessage());
        } catch (SecurityException ex) {
            log.warn("Security error on WS message: {}", ex.getMessage());
            sendErrorToUser(principal, "Forbidden: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Error handling WS message", ex);
            sendErrorToUser(principal, "Server error");
        }
    }

    private void sendErrorToUser(Principal principal, String message) {
        try {
            if (principal != null) {
                // send to /user/queue/errors so FE can show toast / dialog
                messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", message);
            }
        } catch (Exception e) {
            log.warn("Failed to send WS error to user: {}", e.getMessage());
        }
    }

    @MessageExceptionHandler
    public void handleException(Exception ex, Principal principal) {
        log.error("STOMP handler exception: {}", ex.getMessage(), ex);
        sendErrorToUser(principal, "Server error: " + ex.getMessage());
    }
}

package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.MessageCreateRequest;
import com.smartquit.smartquitiot.dto.response.ConversationSummaryDTO;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.MessageDTO;
import com.smartquit.smartquitiot.service.ConversationService;
import com.smartquit.smartquitiot.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    @PostMapping("/messages")
    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "Gửi tin nhắn (tự tạo conversation DIRECT nếu chưa có)",
            description = "Nếu conversationId được truyền sẽ gửi vào đó; nếu không thì cần targetUserId để tạo/find direct conversation.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse<MessageDTO>> sendMessage(
            @Valid @RequestBody MessageCreateRequest req) {

        try {
            MessageDTO dto = messageService.sendMessage(req); // service resolves current user from SecurityContext
            return ResponseEntity.status(201).body(GlobalResponse.created("Message created", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(GlobalResponse.badRequest(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(GlobalResponse.forbidden(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GlobalResponse.error("Server error: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/{conversationId}/messages")
    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "Lấy lịch sử tin nhắn theo conversation",
            description = "Cursor pagination: beforeId (message id) và limit.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse<List<MessageDTO>>> getMessages(
            @PathVariable int conversationId,
            @RequestParam(required = false) Integer beforeId,
            @RequestParam(defaultValue = "30") int limit) {

        try {
            List<MessageDTO> messages = messageService.getMessages(conversationId, beforeId, limit);
            return ResponseEntity.ok(GlobalResponse.ok("Messages fetched", messages));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(GlobalResponse.notFound(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(GlobalResponse.forbidden(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GlobalResponse.error("Server error: " + e.getMessage(), 500));
        }
    }

    // Inbox
    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "Lấy danh sách conversation (inbox) của user hiện tại")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse<List<ConversationSummaryDTO>>> listConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            List<ConversationSummaryDTO> list = conversationService.listConversationsForCurrentUser(page, size);
            return ResponseEntity.ok(GlobalResponse.ok("Conversations fetched", list));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GlobalResponse.error("Server error: " + e.getMessage(), 500));
        }
    }

    // Mark as read
    @PostMapping("/{conversationId}/read")
    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "Đánh dấu conversation đã đọc (cập nhật participant.lastReadAt)")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse<ConversationSummaryDTO>> markAsRead(
            @PathVariable int conversationId) {

        try {
            ConversationSummaryDTO dto = conversationService.markConversationRead(conversationId);
            return ResponseEntity.ok(GlobalResponse.ok("Conversation marked as read", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(GlobalResponse.notFound(e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(GlobalResponse.forbidden(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GlobalResponse.error("Server error: " + e.getMessage(), 500));
        }
    }
}

package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.GetAllNotificationsRequest;
import com.smartquit.smartquitiot.dto.response.NotificationDTO;
import com.smartquit.smartquitiot.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationsController {
    private final NotificationService notificationService;

    @PostMapping("/all")
    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "get all my notifications, paging")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Page<NotificationDTO>> getAll(@RequestBody GetAllNotificationsRequest request) {
        log.debug("REST request to fetch notification page: {}, size: {}", request.getPage(), request.getSize());
        return ResponseEntity.ok(notificationService.getAll(request));
    }

    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "mark Read one notification")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable int id) {
        log.info("REST request to mark notification as read: {}", id);
        notificationService.markReadById(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "mark Read ALL notification")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/read-all")
    public ResponseEntity<Integer> markAllRead() {
        log.info("REST request to mark all notifications as read");
        int count = notificationService.markAllRead();
        log.info("Successfully marked {} notifications as read", count);
        return ResponseEntity.ok(count);
    }

    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "Soft delete all notification")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/delete-all")
    public ResponseEntity<Integer> deleteAll() {
        log.warn("REST request to DELETE ALL notifications for current user");
        int count = notificationService.deleteAll();
        log.info("Soft deleted {} notifications", count);
        return ResponseEntity.ok(count);
    }

    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "Soft delete one notification")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOne(@PathVariable int id) {
        log.info("REST request to delete notification ID: {}", id);
        notificationService.deleteOne(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/mine/appointments")
    @PreAuthorize("hasAnyRole('COACH','MEMBER')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get appointment-related notifications")
    public ResponseEntity<Page<NotificationDTO>> getAppointmentNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isRead
    ) {
        log.debug("REST request for appointment notifications. Page: {}, IsRead: {}", page, isRead);
        GetAllNotificationsRequest req = new GetAllNotificationsRequest();
        req.setPage(page);
        req.setSize(size);
        req.setIsRead(isRead);
        return ResponseEntity.ok(notificationService.getAppointmentNotifications(req));
    }

    @PostMapping("/system-activity/test")
    public ResponseEntity<?> sendSystemActivityNotificationTest(
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "content", required = false) String content
    ) {
        log.info("TEST: Triggering manual system notification - Title: {}", title);
        return ResponseEntity.ok(notificationService.sendSystemActivityNotification(title, content));
    }

    @GetMapping("/system-activity")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get system activity notifications, paging")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Page<NotificationDTO>> getSystemActivityNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("ADMIN ACCESS: Fetching system activity notifications - Page: {}", page);
        return ResponseEntity.ok(notificationService.getSystemNotifications(page, size));
    }
}
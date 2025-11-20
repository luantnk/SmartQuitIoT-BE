package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.CreateNewsRequest;
import com.smartquit.smartquitiot.dto.request.GetAllNotificationsRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.NewsDTO;
import com.smartquit.smartquitiot.dto.response.NotificationDTO;
import com.smartquit.smartquitiot.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationsController {git
    private final NotificationService notificationService;

    @PostMapping("/all")
    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "get all my notifications, paging")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Page<NotificationDTO>> getAll(@RequestBody GetAllNotificationsRequest request) {
        return ResponseEntity.ok(notificationService.getAll(request));

    }

    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "mark Read one notification")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable int id) {
        notificationService.markReadById(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "mark Read ALL notification")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/read-all")
    public ResponseEntity<Integer> markAllRead() {
        int count = notificationService.markAllRead();
        return ResponseEntity.ok(count); // trả số lượng bản ghi được cập nhật
    }

    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "Soft delete all notification")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/delete-all")
    public ResponseEntity<Integer> deleteAll() {
        int count = notificationService.deleteAll();
        return ResponseEntity.ok(count);
    }

    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "Soft delete one notification")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOne(@PathVariable int id) {
        notificationService.deleteOne(id);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/mine/appointments")
    @PreAuthorize("hasAnyRole('COACH','MEMBER')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get appointment-related notifications (booked/cancelled/reminder) for current user")
    public ResponseEntity<Page<NotificationDTO>> getAppointmentNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean isRead
    ) {
        GetAllNotificationsRequest req = new GetAllNotificationsRequest();
        req.setPage(page);
        req.setSize(size);
        req.setIsRead(isRead);
        Page<NotificationDTO> result = notificationService.getAppointmentNotifications(req);
        return ResponseEntity.ok(result);
    }

}


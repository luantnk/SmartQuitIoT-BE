package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.FeedbackRequest;
import com.smartquit.smartquitiot.dto.response.FeedbackResponse;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/appointments/{appointmentId}/feedback")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Member: submit feedback for a completed appointment")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> createFeedback(
            @PathVariable int appointmentId,
            @Valid @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(401).body(GlobalResponse.error("accountId not found in token", 401));
        }
        int memberAccountId = accountIdNum.intValue();

        try {
            feedbackService.createFeedback(appointmentId, memberAccountId, request);
            return ResponseEntity.ok(GlobalResponse.ok("Feedback submitted")); // <-- no entity returned
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(GlobalResponse.error(e.getMessage(), 400));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(GlobalResponse.error(e.getMessage(), 403));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(GlobalResponse.error(e.getMessage(), 409));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GlobalResponse.error("Server error: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/appointments/{appointmentId}/feedback")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Member: Lấy chi tiết feedback đã gửi cho appointment",
            description = "Member có thể xem feedback mà mình đã gửi cho appointment cụ thể. Chỉ member owner của appointment mới có quyền xem.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> getFeedbackByAppointmentId(
            @PathVariable int appointmentId,
            @AuthenticationPrincipal Jwt jwt) {

        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(401).body(GlobalResponse.error("accountId not found in token", 401));
        }
        int memberAccountId = accountIdNum.intValue();

        try {
            FeedbackResponse feedback = feedbackService.getFeedbackByAppointmentIdForMember(appointmentId, memberAccountId);
            return ResponseEntity.ok(GlobalResponse.ok("Feedback fetched", feedback));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(GlobalResponse.error(e.getMessage(), 404));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(GlobalResponse.error(e.getMessage(), 403));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(GlobalResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GlobalResponse.error("Server error: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/admin/coaches/{coachId}/feedbacks")
    @Operation(summary = "Admin: Danh sách feedback của coach truyền coachId (nhớ phân trang)")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse> getFeedbacksForCoachByAdmin(
            @PathVariable int coachId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FeedbackResponse> result = feedbackService.getFeedbacksByCoachId(coachId, pr);
        return ResponseEntity.ok(GlobalResponse.ok(result));
    }

    @GetMapping("/coach/feedbacks")
    @Operation(summary = "Coach: lấy danh sách Feedback")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> getFeedbacksForCoach(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(401).body(GlobalResponse.error("accountId not found in token", 401));
        }
        int accountId = accountIdNum.intValue();

        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FeedbackResponse> result = feedbackService.getFeedbacksForCoachAccount(accountId, pr);
        return ResponseEntity.ok(GlobalResponse.ok(result));
    }

}

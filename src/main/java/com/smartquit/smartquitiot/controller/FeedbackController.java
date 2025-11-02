package com.smartquit.smartquitiot.controller;
import com.smartquit.smartquitiot.dto.request.FeedbackRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.entity.Feedback;
import com.smartquit.smartquitiot.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/appointments/{appointmentId}/feedback")
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
}

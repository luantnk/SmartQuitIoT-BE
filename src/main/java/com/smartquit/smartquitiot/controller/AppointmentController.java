package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.AppointmentRequest;
import com.smartquit.smartquitiot.dto.request.ReassignAppointmentRequest;
import com.smartquit.smartquitiot.dto.request.SnapshotUploadRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.JoinTokenResponse;
import com.smartquit.smartquitiot.enums.AppointmentStatus;
import com.smartquit.smartquitiot.service.AppointmentService;
import com.smartquit.smartquitiot.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final ScheduleService scheduleService;

    @PostMapping
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> bookAppointment(
            @Valid @RequestBody AppointmentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        int accountId = getAccountId(jwt);
        log.info("REST request to book appointment for accountId: {} on date: {}", accountId, request.getDate());

        var response = appointmentService.bookAppointment(accountId, request);
        log.info("Appointment successfully booked for accountId: {}", accountId);
        return ResponseEntity.ok(GlobalResponse.ok("Booking successful", response));
    }

    @DeleteMapping("/{appointmentId}")
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> cancelByMember(
            @PathVariable int appointmentId,
            @AuthenticationPrincipal Jwt jwt) {

        int accountId = getAccountId(jwt);
        log.warn("REST request from MEMBER: {} to cancel appointment: {}", accountId, appointmentId);

        appointmentService.cancelAppointment(appointmentId, accountId);
        return ResponseEntity.ok(GlobalResponse.ok("Cancellation successful", null));
    }

    @DeleteMapping("/{appointmentId}/by-coach")
    @PreAuthorize("hasRole('COACH')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> cancelByCoach(
            @PathVariable int appointmentId,
            @AuthenticationPrincipal Jwt jwt) {

        int accountId = getAccountId(jwt);
        log.warn("REST request from COACH: {} to cancel appointment: {}", accountId, appointmentId);

        appointmentService.cancelAppointmentByCoach(appointmentId, accountId);
        return ResponseEntity.ok(GlobalResponse.ok("Cancellation by coach successful", null));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> listForCurrentUser(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        int accountId = getAccountId(jwt);
        boolean isCoach = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COACH"));

        log.debug("REST request to list appointments for accountId: {} (Role: {})", accountId, isCoach ? "COACH" : "MEMBER");

        Object res = isCoach
                ? appointmentService.getAppointmentsByCoachAccountId(accountId, status, date, page, size)
                : appointmentService.getAppointmentsByMemberAccountId(accountId, status, date, page, size);

        return ResponseEntity.ok(GlobalResponse.ok("Appointments fetched", res));
    }

    @PostMapping("/{appointmentId}/join-token")
    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> getJoinToken(
            @PathVariable int appointmentId,
            @AuthenticationPrincipal Jwt jwt) {

        int accountId = getAccountId(jwt);
        log.info("Generating Agora Join Token for appointment: {} by account: {}", appointmentId, accountId);

        JoinTokenResponse dto = appointmentService.generateJoinTokenForAppointment(appointmentId, accountId);
        return ResponseEntity.ok(GlobalResponse.ok("Join token created", dto));
    }

    @PutMapping("/{appointmentId}/complete")
    @PreAuthorize("hasRole('COACH')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> completeByCoach(
            @PathVariable int appointmentId,
            @AuthenticationPrincipal Jwt jwt) {

        int accountId = getAccountId(jwt);
        log.info("COACH ACCESS: Marking appointment {} as COMPLETED by coach: {}", appointmentId, accountId);

        appointmentService.completeAppointmentByCoach(appointmentId, accountId);
        return ResponseEntity.ok(GlobalResponse.ok("Appointment marked as COMPLETED", null));
    }

    @PutMapping("/{appointmentId}/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> reassignAppointment(
            @PathVariable int appointmentId,
            @RequestBody ReassignAppointmentRequest request) {

        log.info("ADMIN ACCESS: Reassigning appointment: {} to new coach: {}", appointmentId, request.getTargetCoachId());
        appointmentService.reassignAppointment(appointmentId, request.getTargetCoachId());
        return ResponseEntity.ok(GlobalResponse.ok("Appointment reassigned successfully", null));
    }

    @GetMapping("/available-coaches")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> getAvailableCoaches(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam int slotId,
            @RequestParam(required = false) Integer excludeCoachId) {

        log.debug("REST request to find available coaches for date: {} and slot: {}", date, slotId);
        var list = scheduleService.findAvailableCoaches(date, slotId, excludeCoachId);
        return ResponseEntity.ok(GlobalResponse.ok("Available coaches fetched", list));
    }

    /**
     * Helper to extract accountId and log errors if missing.
     * In a real project, this logic could be moved to a custom ArgumentResolver.
     */
    private int getAccountId(Jwt jwt) {
        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            log.error("Critical Security Error: accountId missing from JWT claims!");
            throw new SecurityException("Unauthorized: accountId claim missing");
        }
        return accountIdNum.intValue();
    }
}
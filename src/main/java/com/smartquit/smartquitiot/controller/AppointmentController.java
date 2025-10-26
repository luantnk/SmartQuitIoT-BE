package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.AppointmentRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Member đặt lịch hẹn với coach")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> bookAppointment(
            @Valid @RequestBody AppointmentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(GlobalResponse.error("Không xác định được member từ token", 404));
        }
        int memberId = accountIdNum.intValue();

        var response = appointmentService.bookAppointment(memberId, request);
        return ResponseEntity.ok(GlobalResponse.ok("Đặt lịch thành công", response));
    }

    @DeleteMapping("/{appointmentId}")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Member huỷ lịch hẹn đã đặt")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> cancelAppointment(
            @PathVariable int appointmentId,
            @AuthenticationPrincipal Jwt jwt) {

        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(GlobalResponse.error("Không xác định được member từ token", 404));
        }
        int memberId = accountIdNum.intValue();

        appointmentService.cancelAppointment(appointmentId, memberId);
        return ResponseEntity.ok(GlobalResponse.ok("Huỷ lịch thành công", null));
    }

    @GetMapping
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Lấy danh sách lịch hẹn của member (kèm trạng thái runtime)")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> getAppointmentsForCurrentMember(@AuthenticationPrincipal Jwt jwt) {
        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(GlobalResponse.error("Không xác định được member từ token", 404));
        }
        int memberId = accountIdNum.intValue();

        var responses = appointmentService.getAppointmentsByMemberId(memberId);
        return ResponseEntity.ok(GlobalResponse.ok("Lấy danh sách lịch hẹn thành công", responses));
    }

}

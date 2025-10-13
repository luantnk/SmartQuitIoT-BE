package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.ScheduleAssignRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/schedules")
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;

    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Assign coaches to work dates"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> assignCoachesToDates(@RequestBody ScheduleAssignRequest request) {
        int createdCount = scheduleService.assignCoachesToDates(request);

        return ResponseEntity.ok(GlobalResponse.ok("Assign success", createdCount));
    }
}

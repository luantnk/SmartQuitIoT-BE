package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.ScheduleAssignRequest;
import com.smartquit.smartquitiot.dto.request.ScheduleUpdateRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;

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
    public ResponseEntity<GlobalResponse> assignCoachesToDates(@Valid  @RequestBody ScheduleAssignRequest request) {
        int createdCount = scheduleService.assignCoachesToDates(request);

        return ResponseEntity.ok(GlobalResponse.ok("Assign success", createdCount));
    }
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get assigned schedules by month (only current & future)")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> getSchedulesByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        var result = scheduleService.getSchedulesByMonth(year, month);
        return ResponseEntity.ok(GlobalResponse.ok("Fetched schedules successfully", result));
    }
    @PutMapping("/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update schedule for specific date (add/remove coaches)")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> updateScheduleByDate(
            @PathVariable LocalDate date,
            @Valid @RequestBody ScheduleUpdateRequest request) {

        scheduleService.updateScheduleByDate(date, request);
        return ResponseEntity.ok(GlobalResponse.ok("Cập nhật lịch thành công", null));
    }


}

package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.ScheduleAssignRequest;
import com.smartquit.smartquitiot.dto.request.ScheduleUpdateRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;
    private final AccountService accountService;
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
        return ResponseEntity.ok(GlobalResponse.ok("Update successfully", null));
    }
//    @GetMapping("/coach/{accountId}/workdays")
//    @PreAuthorize("hasRole('ADMIN')")
//    @Operation(summary = "lấy ngày làm việc của coach theo accountId")
//    @SecurityRequirement(name = "Bearer Authentication")
//    public ResponseEntity<GlobalResponse> getWorkdaysByAccount(
//            @PathVariable int accountId,
//            @RequestParam int year,
//            @RequestParam int month) {
//
//        List<LocalDate> days = scheduleService.getWorkdaysByMonth(accountId, year, month);
//        return ResponseEntity.ok(GlobalResponse.ok("Fetched workdays", days));
//    }

    /**
     * Coach: get own workdays (no need to send accountId from FE)
     * Example: GET /schedules/me/workdays?year=2025&month=11
     */
    @GetMapping("/me/workdays")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Get authenticated coach's workdays for a month")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> getMyWorkdays(
            @RequestParam int year,
            @RequestParam int month) {

        var account = accountService.getAuthenticatedAccount();
        int accountId = account.getId();
        List<java.time.LocalDate> days = scheduleService.getWorkdaysByMonth(accountId, year, month);
        return ResponseEntity.ok(GlobalResponse.ok("Fetched my workdays", days));
    }

}

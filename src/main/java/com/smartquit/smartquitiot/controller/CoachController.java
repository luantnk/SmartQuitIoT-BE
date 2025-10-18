package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.response.CoachDTO;
import com.smartquit.smartquitiot.dto.response.CoachSummaryDTO;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.entity.Coach;
import com.smartquit.smartquitiot.service.CoachService;
import com.smartquit.smartquitiot.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/coaches")
@RequiredArgsConstructor
public class CoachController {

    private final CoachService coachService;
    private final ScheduleService scheduleService;
    @GetMapping("/p")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "This endpoint for get authenticated coach")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<CoachDTO> getAuthenticatedCoach(){
        return ResponseEntity.ok(coachService.getAuthenticatedCoachProfile());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @Operation(summary = "Get all coaches (admin,member)", description = "Returns full list of CoachSummaryDTO, no filtering/paging")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse<List<CoachSummaryDTO>>> getAllCoaches() {
        List<CoachSummaryDTO> coaches = coachService.getCoachList();
        return ResponseEntity.ok(GlobalResponse.ok(coaches));
    }
    @GetMapping("/{coachId}/slots/available")
    @PreAuthorize("hasAnyRole('MEMBER','ADMIN')")
    @Operation(summary = "Get available slots of a coach by date (for member or admin)")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> getAvailableSlots(
            @PathVariable int coachId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        var slots = scheduleService.getAvailableSlots(coachId, date);
        return ResponseEntity.ok(GlobalResponse.ok("Fetched available slots successfully", slots));
    }
}

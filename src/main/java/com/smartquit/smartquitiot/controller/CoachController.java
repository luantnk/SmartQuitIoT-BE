package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.CoachUpdateRequest;
import com.smartquit.smartquitiot.dto.response.CoachDTO;
import com.smartquit.smartquitiot.dto.response.CoachSummaryDTO;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.service.CoachService;
import com.smartquit.smartquitiot.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
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

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all coaches (admin)", description = "Returns full list of CoachDTO filtering/paging")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse<Page<CoachDTO>>> getAllCoaches(@RequestParam(name = "page", defaultValue = "0") int page,
                                                                        @RequestParam(name = "size", defaultValue = "10") int size,
                                                                        @RequestParam(name = "searchString", required = false) String searchString,
                                                                        @RequestParam(name = "sortBy", defaultValue = "ASC") Sort.Direction sortBy) {

        return ResponseEntity.ok(GlobalResponse.ok(coachService.getAllCoaches(page, size, searchString, sortBy)));
    }

    @GetMapping("/{coachId}")
    @Operation(summary = "This endpoint for get coach by Coach Id")
    public ResponseEntity<CoachDTO> getCoachById(@PathVariable int coachId) {
        return ResponseEntity.ok(coachService.getCoachById(coachId));
    }

    @PutMapping("/{coachId}")
    @Operation(summary = "This endpoint for update coach profile", description = "Admin and Coaches can update their own profile")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<CoachDTO> updateMemberProfile(@PathVariable int coachId,
                                                         @RequestBody CoachUpdateRequest request) {
        return ResponseEntity.ok(coachService.updateProfile(coachId, request));
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

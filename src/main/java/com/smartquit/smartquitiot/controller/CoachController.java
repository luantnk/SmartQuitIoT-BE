package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.response.CoachDTO;
import com.smartquit.smartquitiot.dto.response.CoachSummaryDTO;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.entity.Coach;
import com.smartquit.smartquitiot.service.CoachService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/coaches")
@RequiredArgsConstructor
public class CoachController {

    private final CoachService coachService;

    @GetMapping("/p")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "This endpoint for get authenticated coach")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<CoachDTO> getAuthenticatedCoach(){
        return ResponseEntity.ok(coachService.getAuthenticatedCoachProfile());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all coaches (admin)", description = "Returns full list of CoachSummaryDTO, no filtering/paging")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse<List<CoachSummaryDTO>>> getAllCoaches() {
        List<CoachSummaryDTO> coaches = coachService.getCoachList();
        return ResponseEntity.ok(GlobalResponse.ok(coaches));
    }
}

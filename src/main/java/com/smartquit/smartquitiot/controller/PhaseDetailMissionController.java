package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.CompleteMissionRequest;
import com.smartquit.smartquitiot.dto.response.QuitPlanResponse;
import com.smartquit.smartquitiot.service.PhaseDetailMissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/phase-detail-mission")
@RequiredArgsConstructor
public class PhaseDetailMissionController {
    private final PhaseDetailMissionService phaseDetailMissionService;

    @PostMapping("/complete")
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "When member complete mission")
    public ResponseEntity<QuitPlanResponse> completePhaseDetailMission(@RequestBody CompleteMissionRequest req) {
        return ResponseEntity.ok(phaseDetailMissionService.completePhaseDetailMission(req));
    }
}

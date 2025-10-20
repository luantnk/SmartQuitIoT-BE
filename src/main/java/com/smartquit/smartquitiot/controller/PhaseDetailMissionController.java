package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.CompleteMissionRequest;
import com.smartquit.smartquitiot.dto.response.MissionTodayResponse;
import com.smartquit.smartquitiot.dto.response.QuitPlanResponse;
import com.smartquit.smartquitiot.service.PhaseDetailMissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/phase-detail-mission")
@RequiredArgsConstructor
public class PhaseDetailMissionController {
    private final PhaseDetailMissionService phaseDetailMissionService;

    @PostMapping("/complete")
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "When member complete mission in VIEW QUIT PLAN DETAIL")
    public ResponseEntity<QuitPlanResponse> completePhaseDetailMission(@RequestBody CompleteMissionRequest req) {
        return ResponseEntity.ok(phaseDetailMissionService.completePhaseDetailMission(req));
    }

    @GetMapping("/mission-today")
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "get list mission today in phase at home page")
    public ResponseEntity<MissionTodayResponse> getListMissionToday() {
        return ResponseEntity.ok(phaseDetailMissionService.getListMissionToday());
    }

    @PostMapping("/complete/home-page")
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "When member complete mission in HOME PAGE AT MISSION TODAY")
    public ResponseEntity<MissionTodayResponse> completePhaseDetailMissionAtHomePage(@RequestBody CompleteMissionRequest req) {
        return ResponseEntity.ok(phaseDetailMissionService.completePhaseDetailMissionAtHomePage(req));
    }

}

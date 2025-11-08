package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.CreateNewQuitPlanRequest;
import com.smartquit.smartquitiot.dto.request.CreateQuitPlanInFirstLoginRequest;
import com.smartquit.smartquitiot.dto.request.KeepPhaseOfQuitPlanRequest;
import com.smartquit.smartquitiot.dto.response.PhaseBatchMissionsResponse;
import com.smartquit.smartquitiot.dto.response.QuitPlanResponse;
import com.smartquit.smartquitiot.dto.response.TimeResponse;
import com.smartquit.smartquitiot.service.QuitPlanService;
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
@RequestMapping("/quit-plan")
@RequiredArgsConstructor
public class QuitPlanController {
    private final QuitPlanService quitPlanService;

    @PostMapping("/create-in-first-login")
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "When first login, this end point will create first quit plan and form metric, after that create phase,phase detail and assign missions for each phase detail ")
    public ResponseEntity<PhaseBatchMissionsResponse> createQuitPlanInFirstLogin(@RequestBody CreateQuitPlanInFirstLoginRequest req){
        PhaseBatchMissionsResponse response = quitPlanService.createQuitPlanInFirstLogin(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping()
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Get information of current quit plan ")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<QuitPlanResponse> getQuitPlan() {
        QuitPlanResponse response = quitPlanService.getCurrentQuitPlan();
        return ResponseEntity.ok(response);
    }


    @GetMapping("/time")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Get time of current quit plan ")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<TimeResponse> getCurrentTimeOfQuitPlan() {
        TimeResponse response = quitPlanService.getCurrentTimeOfQuitPlan();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "Get information of current quit plan of member by memberId")
    public ResponseEntity<QuitPlanResponse> getQuitPlanByMemberId(@PathVariable int memberId) {
        QuitPlanResponse response = quitPlanService.getMemberQuitPlan(memberId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/keep-plan")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Member decide keep quit plan in Failed Phase ")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<QuitPlanResponse> keepPhaseOfQuitPlan(@RequestBody KeepPhaseOfQuitPlanRequest req) {
        QuitPlanResponse response = quitPlanService.keepPhaseOfQuitPlan(req);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-new")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Member choose create NEW QUIT PLAN ")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<PhaseBatchMissionsResponse> createNew(@RequestBody CreateNewQuitPlanRequest req) {
        PhaseBatchMissionsResponse response = quitPlanService.createNewQuitPlan(req);
        return ResponseEntity.ok(response);
    }


}

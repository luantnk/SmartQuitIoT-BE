package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.CreateNewQuitPlanRequest;
import com.smartquit.smartquitiot.dto.request.CreateQuitPlanInFirstLoginRequest;
import com.smartquit.smartquitiot.dto.request.KeepPhaseOfQuitPlanRequest;
import com.smartquit.smartquitiot.dto.response.AiPredictionResponse;
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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/quit-plan")
@RequiredArgsConstructor
public class QuitPlanController {
    private final QuitPlanService quitPlanService;

    @PostMapping("/create-in-first-login")
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "When first login, this end point will create first quit plan...")
    public ResponseEntity<PhaseBatchMissionsResponse> createQuitPlanInFirstLogin(@RequestBody CreateQuitPlanInFirstLoginRequest req) {
        log.info("REST request to create initial QuitPlan: {}", req.getQuitPlanName());
        PhaseBatchMissionsResponse response = quitPlanService.createQuitPlanInFirstLogin(req);
        log.info("Successfully created initial QuitPlan for request: {}", req.getQuitPlanName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping()
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Get information of current quit plan ")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<QuitPlanResponse> getQuitPlan() {
        log.debug("REST request to get current QuitPlan");
        QuitPlanResponse response = quitPlanService.getCurrentQuitPlan();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/time")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Get time of current quit plan ")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<TimeResponse> getCurrentTimeOfQuitPlan() {
        log.debug("REST request to get current time of QuitPlan");
        TimeResponse response = quitPlanService.getCurrentTimeOfQuitPlan();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "Get information of current quit plan of member by memberId")
    public ResponseEntity<QuitPlanResponse> getQuitPlanByMemberId(@PathVariable int memberId) {
        log.info("REST request to get QuitPlan for memberId: {}", memberId);
        QuitPlanResponse response = quitPlanService.getMemberQuitPlan(memberId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/keep-plan")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Member decide keep quit plan in Failed Phase ")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<QuitPlanResponse> keepPhaseOfQuitPlan(@RequestBody KeepPhaseOfQuitPlanRequest req) {
        log.info("REST request to keep Phase: {} for Plan: {}", req.getPhaseId(), req.getQuitPlanId());
        QuitPlanResponse response = quitPlanService.keepPhaseOfQuitPlan(req);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-new")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Member choose create NEW QUIT PLAN ")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<PhaseBatchMissionsResponse> createNew(@RequestBody CreateNewQuitPlanRequest req) {
        log.info("REST request to create NEW QuitPlan: {}", req.getQuitPlanName());
        PhaseBatchMissionsResponse response = quitPlanService.createNewQuitPlan(req);
        log.info("Successfully started new QuitPlan journey: {}", req.getQuitPlanName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all-quit-plan")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Member get all my quit plan to view HISTORY ")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<QuitPlanResponse>> getAllQuitPLan() {
        log.debug("REST request to get QuitPlan history");
        List<QuitPlanResponse> response = quitPlanService.getHistory();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/specific/{id}")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Member get owner quit plan by Id ")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<QuitPlanResponse> getSpecificQuitPlanById(@PathVariable int id) {
        log.debug("REST request to get specific QuitPlan by ID: {}", id);
        QuitPlanResponse response = quitPlanService.getSpecific(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/prediction")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Get AI Prediction for success probability of current plan")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<AiPredictionResponse> getPlanPrediction() {
        log.info("REST request to get AI Prediction for current QuitPlan");
        AiPredictionResponse response = quitPlanService.getPredictionForCurrentPlan();
        return ResponseEntity.ok(response);
    }
}
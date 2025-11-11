package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.TestConditionRequest;
import com.smartquit.smartquitiot.dto.request.UpdateSystemPhaseConditionRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.SlotDTO;
import com.smartquit.smartquitiot.dto.response.SystemPhaseConditionDTO;
import com.smartquit.smartquitiot.dto.response.TestConditionResponse;
import com.smartquit.smartquitiot.service.SystemPhaseConditionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system-phase-condition")
@RequiredArgsConstructor
public class SystemPhaseConditionController {
    private final SystemPhaseConditionService systemPhaseConditionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get all System Phase Condition", description = "Admin get 5 condition")
    public ResponseEntity<List<SystemPhaseConditionDTO>> getAllSlots() {
        return ResponseEntity.ok(systemPhaseConditionService.getAllSystemPhaseCondition());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin update system phase condition")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<SystemPhaseConditionDTO> updateSystemPhaseCondition(
            @PathVariable Integer id,
            @RequestBody UpdateSystemPhaseConditionRequest request) {
        SystemPhaseConditionDTO response = systemPhaseConditionService.updateSystemPhaseCondition(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Test if a condition passes with given test data")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<TestConditionResponse> testCondition(
            @RequestBody TestConditionRequest request) {
        TestConditionResponse response = systemPhaseConditionService.testCondition(request);
        return ResponseEntity.ok(response);
    }
}

package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.RedoPhaseRequest;
import com.smartquit.smartquitiot.dto.response.PhaseDTO;
import com.smartquit.smartquitiot.dto.response.QuitPlanResponse;
import com.smartquit.smartquitiot.service.PhaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("phase")
public class PhaseController {
    private final PhaseService  phaseService;

    //home page
    @GetMapping("/home-page")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Get information of current Phase at Home Page ")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<PhaseDTO> getCurrentPhaseAtHomePage() {
        PhaseDTO response = phaseService.getCurrentPhaseAtHomePage();
        return ResponseEntity.ok(response);
    }


    //home page
    @PostMapping("/redo")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Member redo phase failed")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<QuitPlanResponse> redPhase(@RequestBody RedoPhaseRequest redoPhaseRequest) {
        QuitPlanResponse response = phaseService.redoPhaseInFailed(redoPhaseRequest);
        return ResponseEntity.ok(response);
    }


}

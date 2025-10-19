package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.response.PhaseDTO;
import com.smartquit.smartquitiot.dto.response.QuitPlanResponse;
import com.smartquit.smartquitiot.repository.PhaseRepository;
import com.smartquit.smartquitiot.service.PhaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}

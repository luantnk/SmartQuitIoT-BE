package com.smartquit.smartquitiot.controller;

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

@RestController
@RequestMapping("/coaches")
@RequiredArgsConstructor
public class CoachController {

    private final CoachService coachService;

    @GetMapping("/p")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "This endpoint for get authenticated coach")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Coach> getAuthenticatedCoach(){
        return ResponseEntity.ok(coachService.getAuthenticatedCoach());
    }
}

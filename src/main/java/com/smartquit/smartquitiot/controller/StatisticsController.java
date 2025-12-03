package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.response.DashboardStatisticsDTO;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','COACH')")
    @Operation(summary = "Get dashboard statistics", 
               description = "Returns statistics for appointments and members including today's appointments, pending requests, completed this week, active members, and upcoming appointments")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse<DashboardStatisticsDTO>> getDashboardStatistics() {
        DashboardStatisticsDTO statistics = statisticsService.getDashboardStatistics();
        return ResponseEntity.ok(GlobalResponse.ok("Dashboard statistics fetched successfully", statistics));
    }
}

package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.response.MetricDTO;
import com.smartquit.smartquitiot.service.MetricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final MetricService metricService;

    @GetMapping("/home-screen")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get metric statistic for the authenticated member",
            description = "API để hiển thị trạng thái của member trên home screen")
    public ResponseEntity<MetricDTO> getHomeScreenMetrics() {
        return ResponseEntity.ok(metricService.getHomeScreenMetrics());
    }

    @GetMapping("/health-data")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get health data statistic for the authenticated member",
            description = "API để lấy dữ liệu sức khỏe của member")
    public ResponseEntity<Map<String, Object>> getHealthData() {
        return ResponseEntity.ok(metricService.getHealthMetrics());
    }

}

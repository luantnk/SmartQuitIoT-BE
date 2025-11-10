package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.CreateNewQuitPlanRequest;
import com.smartquit.smartquitiot.dto.request.UpdateFormMetricRequest;
import com.smartquit.smartquitiot.dto.response.*;
import com.smartquit.smartquitiot.service.FormMetricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/form-metric")
public class FormMetricController {
    private final FormMetricService formMetricService;
    //update form metric -> tao lai quit plan
    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "Get Current Form Metric ")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GetFormMetricResponse> getMyCurrentFormMetric() {
        GetFormMetricResponse response = formMetricService.getMyFormMetric();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "UPDATE Form metric. This can influence with Quit Plan ")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<UpdateFormMetricResponse> updateMyCurrentFormMetric(@RequestBody UpdateFormMetricRequest updateFormMetricRequest) {
        UpdateFormMetricResponse response = formMetricService.updateMyFormMetric(updateFormMetricRequest);
        return ResponseEntity.ok(response);
    }

}

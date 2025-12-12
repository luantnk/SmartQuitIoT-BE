package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.response.MissionTypeDTO;
import com.smartquit.smartquitiot.service.MissionTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/mission-type")
@RequiredArgsConstructor
public class MissionTypeController {
    private final MissionTypeService  missionTypeService;

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER','COACH')")
    @Operation(summary = "Get all Mission Types")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<MissionTypeDTO>> getAll() {
        return ResponseEntity.ok(missionTypeService.getAllMissionTypes());
    }
}

package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.CreateMissionRequest;
import com.smartquit.smartquitiot.dto.request.UpdateMissionRequest;
import com.smartquit.smartquitiot.dto.response.AchievementDTO;
import com.smartquit.smartquitiot.dto.response.MissionDTO;
import com.smartquit.smartquitiot.service.MissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get all mission", description = "Returns full list of missions filtering/paging")
    public ResponseEntity<Page<MissionDTO>> getAllMissions(@RequestParam(name = "page", defaultValue = "0") int page,
                                                           @RequestParam(name = "size", defaultValue = "10") int size,
                                                           @RequestParam(required = false) String search,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) String phase) {
        return ResponseEntity.ok(missionService.getAllMissions(page,size,search,status,phase));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "This end point for admin get detail a mission")
    public ResponseEntity<MissionDTO> getDetailsById(@PathVariable int id){
        return ResponseEntity.ok(missionService.getDetails(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "This end point for admin delete mission")
    public ResponseEntity<MissionDTO> delete(@PathVariable int id){
        return ResponseEntity.ok(missionService.deleteMission(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Create new mission", description = "Create a new mission with validation for unique code")
    public ResponseEntity<MissionDTO> createMission(@Valid @RequestBody CreateMissionRequest request) {
            return ResponseEntity.status(HttpStatus.CREATED).body(missionService.createMission(request));
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Update mission", description = "Update an existing mission with validation for unique code")
    public ResponseEntity<MissionDTO> updateMission(@PathVariable int id,
                                           @Valid @RequestBody UpdateMissionRequest request) {
            MissionDTO updatedMission = missionService.updateMission(id, request);
            return ResponseEntity.ok(updatedMission);
    }
}

package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.CreateAchievementRequest;
import com.smartquit.smartquitiot.dto.request.UpdateReminderTemplateRequest;
import com.smartquit.smartquitiot.dto.response.AchievementDTO;
import com.smartquit.smartquitiot.dto.response.ReminderTemplateDTO;
import com.smartquit.smartquitiot.service.ReminderTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reminder")
@RequiredArgsConstructor
public class ReminderTemplateController {

    private final ReminderTemplateService reminderTemplateService;


    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "This end point for get all reminder template with pagination and search")
    public ResponseEntity<Page<ReminderTemplateDTO>> getReminderTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") int size,
            @RequestParam(defaultValue = "") String search
    ) {
            return ResponseEntity.ok(reminderTemplateService.getAllReminderTemplate(page, size, search));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "This end point for admin update reminder template")
    public ResponseEntity<ReminderTemplateDTO> update(@PathVariable int id,
                                                 @RequestBody UpdateReminderTemplateRequest request){
        return ResponseEntity.ok(reminderTemplateService.updateContent(id,request));
    }
}

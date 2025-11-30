package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.SlotReseedRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.SlotDTO;
import com.smartquit.smartquitiot.dto.response.SlotReseedResponse;
import com.smartquit.smartquitiot.service.SlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;

@RequestMapping("/slots")
@RestController
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    @GetMapping
    @Operation(summary = "Get all Slot", description = "Api này return SlotDTO")
    public ResponseEntity<GlobalResponse<Page<SlotDTO>>> getAllSlots(@RequestParam(name = "page", defaultValue = "0") int page,
                                                                     @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(GlobalResponse.ok("Get all slot success", slotService.listAllSlots(page, size)));
    }

    @PostMapping("/reseed")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Admin reseed slots with new configuration",
            description = "Reseed slots with new start time, end time, slot duration, and gap. " +
                         "Will block if there are active appointments from today onwards. " +
                         "Automatically deletes orphan slots (not used by any schedule)."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse<SlotReseedResponse>> reseedSlots(
            @Valid @RequestBody SlotReseedRequest request) {
        
        LocalTime start = LocalTime.parse(request.getStart());
        LocalTime end = LocalTime.parse(request.getEnd());
        
        SlotReseedResponse response = slotService.reseedSlots(
                start, 
                end, 
                request.getSlotMinutes(), 
                request.getGapMinutes()
        );
        
        String message = String.format(
                "Slots reseeded successfully. Created: %d slots, Deleted: %d orphan slots",
                response.getCreatedCount(),
                response.getDeletedCount()
        );
        
        return ResponseEntity.ok(GlobalResponse.ok(message, response));
    }
}

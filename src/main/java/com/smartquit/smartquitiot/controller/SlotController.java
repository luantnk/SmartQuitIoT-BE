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
        
        // Normalize time format: convert "H:mm" to "HH:mm" for LocalTime.parse()
        String normalizedStart = normalizeTimeFormat(request.getStart());
        String normalizedEnd = normalizeTimeFormat(request.getEnd());
        
        LocalTime start = LocalTime.parse(normalizedStart);
        LocalTime end = LocalTime.parse(normalizedEnd);
        
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
    
    /**
     * Normalize time format from "H:mm" to "HH:mm"
     * Example: "5:30" -> "05:30", "09:30" -> "09:30"
     */
    private String normalizeTimeFormat(String time) {
        if (time == null || time.isEmpty()) {
            return time;
        }
        String[] parts = time.split(":");
        if (parts.length == 2 && parts[0].length() == 1) {
            // Add leading zero if hour has only 1 digit
            return "0" + time;
        }
        return time;
    }
}

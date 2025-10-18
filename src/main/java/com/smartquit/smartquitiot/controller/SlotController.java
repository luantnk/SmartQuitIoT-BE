package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.SlotDTO;
import com.smartquit.smartquitiot.service.SlotService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}

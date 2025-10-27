package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.AddAchievementRequest;
import com.smartquit.smartquitiot.dto.request.MemberAccountRequest;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.entity.Achievement;
import com.smartquit.smartquitiot.entity.MemberAchievement;
import com.smartquit.smartquitiot.service.MemberAchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/achievement")
@RequiredArgsConstructor
public class AchievementController {
    private final MemberAchievementService memberAchievementService;


    @PostMapping()
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER','COACH')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "This end point for test handle achievement and receive notifications by websocket on web")
    public ResponseEntity<Achievement> addMemberAchievements(@RequestBody AddAchievementRequest request){
        Achievement achievement = memberAchievementService.addMemberAchievement(request).orElse(null);
        return ResponseEntity.ok(achievement);
    }

}

package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.AddAchievementRequest;
import com.smartquit.smartquitiot.dto.request.CreateAchievementRequest;
import com.smartquit.smartquitiot.dto.response.AchievementDTO;
import com.smartquit.smartquitiot.dto.response.TopMemberAchievementDTO;
import com.smartquit.smartquitiot.entity.Achievement;
import com.smartquit.smartquitiot.service.AchievementService;
import com.smartquit.smartquitiot.service.MemberAchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/achievement")
@RequiredArgsConstructor
public class AchievementController {
    private final MemberAchievementService memberAchievementService;
    private final AchievementService achievementService;

    @PostMapping("/add-member-achievement")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER','COACH')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Test handle achievement and receive notifications by websocket")
    public ResponseEntity<Achievement> addMemberAchievements(@RequestBody AddAchievementRequest request){
        Achievement achievement = memberAchievementService.addMemberAchievement(request).orElse(null);
        if (achievement != null) {
            log.info("Achievement '{}' successfully granted", achievement.getName());
        }
        return ResponseEntity.ok(achievement);
    }

    @GetMapping("/all-my-achievements")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER','COACH')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<AchievementDTO>> getAllAchievement(){
        log.debug("REST request to get all achievements for current owner");
        return ResponseEntity.ok(memberAchievementService.getAllMyAchievements());
    }

    @GetMapping("/my-achievements-at-home")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER','COACH')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<AchievementDTO>> getAll4Achievement(){
        log.debug("REST request to fetch top 4 achievements for dashboard");
        return ResponseEntity.ok(memberAchievementService.getMyAchievementsAtHome());
    }

    @GetMapping("/top-leader-boards")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER','COACH')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<TopMemberAchievementDTO>> getTop10MembersWithAchievements(){
        log.debug("REST request to fetch top 10 leaderboard");
        return ResponseEntity.ok(memberAchievementService.getTop10MembersWithAchievements());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Page<AchievementDTO>> manageAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search
    ){
        log.info("ADMIN ACCESS: Fetching all achievements for management. Search query: '{}'", search);
        return ResponseEntity.ok(achievementService.getAllAchievements(page, size, search));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<AchievementDTO> getDetails(@PathVariable int id){
        log.debug("REST request to get achievement details for ID: {}", id);
        return ResponseEntity.ok(achievementService.getAchievementById(id));
    }

    @PostMapping("/create-new")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<AchievementDTO> createNewAchievement(@RequestBody CreateAchievementRequest request){
        log.info("ADMIN ACCESS: Creating new achievement: {}", request.getName());
        AchievementDTO response = achievementService.createAchievement(request);
        log.info("Successfully created achievement with ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<AchievementDTO> delete(@PathVariable int id){
        log.warn("ADMIN ACCESS: Deleting achievement ID: {}", id);
        AchievementDTO deleted = achievementService.deleteAchievement(id);
        log.info("Achievement ID: {} soft deleted successfully", id);
        return ResponseEntity.ok(deleted);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<AchievementDTO> update(@PathVariable int id,
                                                 @RequestBody CreateAchievementRequest request){
        log.info("ADMIN ACCESS: Updating achievement ID: {}", id);
        AchievementDTO updated = achievementService.updateAchievement(id, request);
        return ResponseEntity.ok(updated);
    }
}
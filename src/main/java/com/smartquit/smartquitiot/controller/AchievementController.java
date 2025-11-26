package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.AddAchievementRequest;
import com.smartquit.smartquitiot.dto.request.CreateAchievementRequest;
import com.smartquit.smartquitiot.dto.request.MemberAccountRequest;
import com.smartquit.smartquitiot.dto.response.AchievementDTO;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.dto.response.TopMemberAchievementDTO;
import com.smartquit.smartquitiot.entity.Achievement;
import com.smartquit.smartquitiot.entity.MemberAchievement;
import com.smartquit.smartquitiot.service.AchievementService;
import com.smartquit.smartquitiot.service.MemberAchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/achievement")
@RequiredArgsConstructor
public class AchievementController {
    private final MemberAchievementService memberAchievementService;
    private final AchievementService achievementService;


    @PostMapping("/add-member-achievement")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER','COACH')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "This end point for test handle achievement and receive notifications by websocket on web")
    public ResponseEntity<Achievement> addMemberAchievements(@RequestBody AddAchievementRequest request){
        Achievement achievement = memberAchievementService.addMemberAchievement(request).orElse(null);
        return ResponseEntity.ok(achievement);
    }

    @GetMapping("/all-my-achievements")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER','COACH')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "This end point for get all my achievements of Owner ")
    public ResponseEntity<List<AchievementDTO>> getAllAchievement(){
      return ResponseEntity.ok(memberAchievementService.getAllMyAchievements());
    }

    @GetMapping("/my-achievements-at-home")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER','COACH')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "This end point for get 4 my achievements of Owner ")
    public ResponseEntity<List<AchievementDTO>> getAll4Achievement(){
        return ResponseEntity.ok(memberAchievementService.getMyAchievementsAtHome());
    }
    @GetMapping("/top-leader-boards")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER','COACH')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "This end point for get TOP leader boards at Home Page ")
    public ResponseEntity<List<TopMemberAchievementDTO>> getTop10MembersWithAchievements(){
        return ResponseEntity.ok(memberAchievementService.getTop10MembersWithAchievements());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "This end point for get all achievements with pagination and search")
    public ResponseEntity<Page<AchievementDTO>> manageAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search
    ){
        return ResponseEntity.ok(achievementService.getAllAchievements(page, size, search));
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "This end point for get achievements by id")
    public ResponseEntity<AchievementDTO> getDetails(@PathVariable int id){
        return ResponseEntity.ok(achievementService.getAchievementById(id));
    }

    @PostMapping("/create-new")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "This end point for admin create new achievement")
    public ResponseEntity<AchievementDTO> createNewAchievement(@RequestBody CreateAchievementRequest request){
        return ResponseEntity.ok(achievementService.createAchievement(request));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "This end point for admin delete achievement")
    public ResponseEntity<AchievementDTO> delete(@PathVariable int id){
        return ResponseEntity.ok(achievementService.deleteAchievement(id));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "This end point for admin update achievement")
    public ResponseEntity<AchievementDTO> delete( @PathVariable int id,
                                                  @RequestBody CreateAchievementRequest request){
        return ResponseEntity.ok(achievementService.updateAchievement(id,request));
    }
}

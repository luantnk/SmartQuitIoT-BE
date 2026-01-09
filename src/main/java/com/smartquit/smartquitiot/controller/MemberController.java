package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.MemberReminderSettingsRequest;
import com.smartquit.smartquitiot.dto.request.MemberUpdateRequest;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.dto.response.MemberListItemDTO;
import com.smartquit.smartquitiot.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/p")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Get authenticated member profile")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<MemberDTO> getAuthenticatedMember(){
        log.debug("REST request to get current authenticated member profile");
        return ResponseEntity.ok(memberService.getAuthenticatedMemberProfile());
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "Get member by memberId")
    public ResponseEntity<MemberDTO> getMemberById(@PathVariable int memberId){
        log.debug("REST request to get member profile for ID: {}", memberId);
        return ResponseEntity.ok(memberService.getMemberById(memberId));
    }

    @PutMapping
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Update member profile")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<MemberDTO> updateMemberProfile(@RequestBody MemberUpdateRequest request){
        log.info("REST request to update profile for current member");
        MemberDTO updatedMember = memberService.updateProfile(request);
        log.info("Profile updated successfully for Member ID: {}", updatedMember.getId());
        return ResponseEntity.ok(updatedMember);
    }

    @GetMapping("/manage")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all members for admin")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Page<MemberDTO>> manageMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "true") Boolean isActive
    ){
        log.info("ADMIN ACCESS: Managing members list. Page: {}, Search: '{}', IsActive: {}", page, search, isActive);
        return ResponseEntity.ok(memberService.getMembers(page, size, search, isActive));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "API dành cho coach: Lấy danh sách member (tóm tắt)")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<MemberListItemDTO>> getMembersForCoach() {
        log.info("COACH ACCESS: Fetching member summaries for dashboard");
        return ResponseEntity.ok(memberService.getListMembers());
    }

    @PutMapping("/settings/reminder")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Update setting time of noti reminder")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<MemberDTO> updateReminderSettings(@RequestBody MemberReminderSettingsRequest req) {
        log.info("REST request to update reminder settings. Time: {}", req.getMorningReminderTime());
        MemberDTO response = memberService.updateReminderSettings(req);
        log.info("Reminder settings updated successfully");
        return ResponseEntity.ok(response);
    }
}
package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.MemberReminderSettingsRequest;
import com.smartquit.smartquitiot.dto.request.MemberUpdateRequest;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.dto.response.MemberListItemDTO;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/p")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "This endpoint for get authenticated member")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<MemberDTO> getAuthenticatedMember(){
        return ResponseEntity.ok(memberService.getAuthenticatedMemberProfile());
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "This endpoint for get member by memberId")
    public ResponseEntity<MemberDTO> getMemberById(@PathVariable int memberId){
        return ResponseEntity.ok(memberService.getMemberById(memberId));
    }

    @PutMapping
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "This endpoint for update member profile", description = "Only members can update their own profile")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<MemberDTO> updateMemberProfile(@RequestBody MemberUpdateRequest request){
        return ResponseEntity.ok(memberService.updateProfile(request));
    }

    @GetMapping("/manage")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "This endpoint for get all members for admin", description = "Only admin can access this endpoint")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Page<MemberDTO>> manageMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search
    ){
        return ResponseEntity.ok(memberService.getMembers(page, size, search));
    }


        @GetMapping("/summary")
        @PreAuthorize("hasRole('COACH')")
        @Operation(
                summary = "API dành cho coach: Lấy danh sách member (tóm tắt)",
                description = "Trả về danh sách summary (rút gọn) dành cho Coach. " +
                        "Mục đích: gồm tên, avatar, tuổi, và tóm tắt các chỉ số chính (streaks, % smoke-free, % reduction)."
        )
        @SecurityRequirement(name = "Bearer Authentication")
        public ResponseEntity<List<MemberListItemDTO>> getMembersForCoach(
        ) {
            List<MemberListItemDTO> result = memberService.getListMembers();
            return ResponseEntity.ok(result);
        }


    @PutMapping("/settings/reminder")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "This endpoint for update setting time of noti reminder", description = "Only member can access this endpoint")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<MemberDTO> updateReminderSettings(@RequestBody MemberReminderSettingsRequest req) {

        return ResponseEntity.ok(memberService.updateReminderSettings(req));
    }


}

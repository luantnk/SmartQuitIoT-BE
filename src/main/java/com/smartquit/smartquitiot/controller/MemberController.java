package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.MemberUpdateRequest;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}

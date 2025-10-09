package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/p")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "This endpoint for get authenticated member")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Member> getAuthenticatedMember(){
        return ResponseEntity.ok(memberService.getAuthenticatedMember());
    }
}

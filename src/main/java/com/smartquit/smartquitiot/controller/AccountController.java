package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.CoachAccountRequest;
import com.smartquit.smartquitiot.dto.request.MemberAccountRequest;
import com.smartquit.smartquitiot.dto.response.CoachDTO;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/register")
    @Operation(summary = "This endpoint for register new member account")
    public ResponseEntity<MemberDTO> registerMemberAccount(@RequestBody MemberAccountRequest request){
        return ResponseEntity.ok(accountService.registerMember(request));
    }

    @PostMapping("/coach/create")
    @Operation(summary = "This endpoint for create new coach account")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CoachDTO> createCoachAccount(@RequestBody CoachAccountRequest request){
        return ResponseEntity.ok(accountService.registerCoach(request));
    }
}

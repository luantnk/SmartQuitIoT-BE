package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.CoachAccountRequest;
import com.smartquit.smartquitiot.dto.request.MemberAccountRequest;
import com.smartquit.smartquitiot.dto.response.CoachDTO;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/register")
    @Operation(summary = "This endpoint for register new member account")
    public ResponseEntity<MemberDTO> registerMemberAccount(@RequestBody MemberAccountRequest request){
        return new ResponseEntity<>(accountService.registerMember(request), HttpStatus.CREATED);
    }

    @PostMapping("/coach/create")
    @Operation(summary = "This endpoint for create new coach account")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CoachDTO> createCoachAccount(@RequestBody CoachAccountRequest request){
        return new ResponseEntity<>(accountService.registerCoach(request), HttpStatus.CREATED);
    }

    @GetMapping("/p")
    @Operation(summary = "This endpoint for get authenticated account")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Account> getAuthenticatedAccount(){
        return ResponseEntity.ok(accountService.getAuthenticatedAccount());
    }
}

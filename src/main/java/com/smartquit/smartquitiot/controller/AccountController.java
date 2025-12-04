package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.*;
import com.smartquit.smartquitiot.dto.response.CoachDTO;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.dto.response.MessageResponse;
import com.smartquit.smartquitiot.dto.response.VerifyOtpResponse;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
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
    public ResponseEntity<MemberDTO> registerMemberAccount(@RequestBody @Valid MemberAccountRequest request){
        return new ResponseEntity<>(accountService.registerMember(request), HttpStatus.CREATED);
    }

    @PostMapping("/coach/create")
    @Operation(summary = "This endpoint for create new coach account")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CoachDTO> createCoachAccount(@RequestBody @Valid CoachAccountRequest request){
        return new ResponseEntity<>(accountService.registerCoach(request), HttpStatus.CREATED);
    }

    @GetMapping("/p")
    @Operation(summary = "This endpoint for get authenticated account")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Account> getAuthenticatedAccount(){
        return ResponseEntity.ok(accountService.getAuthenticatedAccount());
    }

    @PostMapping("/password/forgot")
    @Operation(summary = "Request a password reset OTP")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        accountService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(new MessageResponse("An OTP has been sent to your email. Please check."));
    }

    @PutMapping("/password/update")
    @Operation(summary = "Update account password")
    public ResponseEntity<MessageResponse> updatePassword(@RequestBody ChangePasswordRequest request) {
        accountService.updatePassword(request);
        return ResponseEntity.ok(new MessageResponse("Password has been changed successfully."));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP and get a reset token")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse response = accountService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset password using the reset token")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Your password has been reset successfully."));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get account statistics")
    public ResponseEntity<?> getAccountStatistics() {
        return ResponseEntity.ok(accountService.getAccountStatistics());
    }

    @PutMapping("/activate/{accountId}")
    @Operation(summary = "Activate account by ID")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> activateAccountById(@PathVariable int accountId) {
        return ResponseEntity.ok(accountService.activeAccountById(accountId));
    }

    @PutMapping("/delete/{accountId}")
    @Operation(summary = "Ban account by ID")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAccountById(@PathVariable int accountId) {
        return ResponseEntity.ok(accountService.deleteAccountById(accountId));
    }
}

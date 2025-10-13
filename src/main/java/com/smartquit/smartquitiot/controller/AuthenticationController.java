package com.smartquit.smartquitiot.controller;

import com.nimbusds.jose.JOSEException;
import com.smartquit.smartquitiot.dto.request.*;
import com.smartquit.smartquitiot.dto.response.AuthenticationResponse;
import com.smartquit.smartquitiot.dto.response.MessageResponse;
import com.smartquit.smartquitiot.dto.response.VerifyOtpResponse;
import com.smartquit.smartquitiot.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/member")
    @Operation(summary = "This endpoint for MEMBER login")
    public ResponseEntity<AuthenticationResponse> memberLogin(@RequestBody AuthenticationRequest request){
        return ResponseEntity.ok(authenticationService.login(request, false));
    }

    @PostMapping("/system")
    @Operation(summary = "This endpoint for COACH and ADMIN login")
    public ResponseEntity<AuthenticationResponse> systemLogin(@RequestBody AuthenticationRequest request){
        return ResponseEntity.ok(authenticationService.login(request, true));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(@RequestBody RefreshTokenRequest request) throws ParseException, JOSEException {
        return ResponseEntity.ok(authenticationService.refreshToken(request));
    }

    @PostMapping("/password/forgot")
    @Operation(summary = "Request a password reset OTP")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authenticationService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(new MessageResponse("An OTP has been sent to your email. Please check."));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP and get a reset token")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse response = authenticationService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset password using the reset token")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Your password has been reset successfully."));
    }

}

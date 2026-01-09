package com.smartquit.smartquitiot.controller;

import com.nimbusds.jose.JOSEException;
import com.smartquit.smartquitiot.dto.request.AuthenticationRequest;
import com.smartquit.smartquitiot.dto.request.GoogleLoginRequest;
import com.smartquit.smartquitiot.dto.request.LogoutRequest;
import com.smartquit.smartquitiot.dto.request.RefreshTokenRequest;
import com.smartquit.smartquitiot.dto.response.AuthenticationResponse;
import com.smartquit.smartquitiot.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/member")
    @Operation(summary = "This endpoint for MEMBER login")
    public ResponseEntity<AuthenticationResponse> memberLogin(@RequestBody @Valid AuthenticationRequest request){
        log.info("Login attempt for member: {}", request.getUsernameOrEmail());
        AuthenticationResponse response = authenticationService.login(request, false);
        log.info("Member successfully authenticated: {}", request.getUsernameOrEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/system")
    @Operation(summary = "This endpoint for COACH and ADMIN login")
    public ResponseEntity<AuthenticationResponse> systemLogin(@RequestBody @Valid AuthenticationRequest request){
        log.warn("System login attempt (Coach/Admin) for user: {}", request.getUsernameOrEmail());
        AuthenticationResponse response = authenticationService.login(request, true);
        log.info("System user successfully authenticated: {}", request.getUsernameOrEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token")
    public ResponseEntity<AuthenticationResponse> refresh(@RequestBody RefreshTokenRequest request)
            throws ParseException, JOSEException {
        log.debug("Token refresh requested");
        return ResponseEntity.ok(authenticationService.refreshToken(request));
    }

    @PostMapping("/google")
    @Operation(summary = "Google OAuth2 login")
    public ResponseEntity<AuthenticationResponse> googleLogin(@RequestBody GoogleLoginRequest request)
            throws GeneralSecurityException, IOException {
        log.info("Google login attempt initiated");
        AuthenticationResponse response = authenticationService.loginWithGoogle(request.getIdToken());
        log.info("Google login successful");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ResponseEntity.ok().build();
    }
}
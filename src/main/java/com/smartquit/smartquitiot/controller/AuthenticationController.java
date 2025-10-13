package com.smartquit.smartquitiot.controller;

import com.nimbusds.jose.JOSEException;
import com.smartquit.smartquitiot.dto.request.AuthenticationRequest;
import com.smartquit.smartquitiot.dto.request.RefreshTokenRequest;
import com.smartquit.smartquitiot.dto.response.AuthenticationResponse;
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
    public ResponseEntity<AuthenticationResponse> memberLogin(@RequestBody @Valid AuthenticationRequest request){
        return ResponseEntity.ok(authenticationService.login(request, false));
    }

    @PostMapping("/system")
    @Operation(summary = "This endpoint for COACH and ADMIN login")
    public ResponseEntity<AuthenticationResponse> systemLogin(@RequestBody @Valid AuthenticationRequest request){
        return ResponseEntity.ok(authenticationService.login(request, true));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(@RequestBody RefreshTokenRequest request) throws ParseException, JOSEException {
        return ResponseEntity.ok(authenticationService.refreshToken(request));
    }

}

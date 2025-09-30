package com.smartquit.smartquitiot.service;

import com.nimbusds.jose.JOSEException;
import com.smartquit.smartquitiot.dto.request.AuthenticationRequest;
import com.smartquit.smartquitiot.dto.request.RefreshTokenRequest;
import com.smartquit.smartquitiot.dto.response.AuthenticationResponse;

import java.text.ParseException;

public interface AuthenticationService {

    AuthenticationResponse login(AuthenticationRequest request);
    AuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest) throws ParseException, JOSEException;
}

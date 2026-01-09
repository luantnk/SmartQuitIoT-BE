package com.smartquit.smartquitiot.service;

import com.nimbusds.jose.JOSEException;
import com.smartquit.smartquitiot.dto.request.AuthenticationRequest;
import com.smartquit.smartquitiot.dto.request.LogoutRequest;
import com.smartquit.smartquitiot.dto.request.RefreshTokenRequest;
import com.smartquit.smartquitiot.dto.response.AuthenticationResponse;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;

public interface AuthenticationService {

    AuthenticationResponse login(AuthenticationRequest request, boolean isSystem);
    AuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest) throws ParseException, JOSEException;
    AuthenticationResponse loginWithGoogle(String idToken) throws GeneralSecurityException, IOException;
    void logout(LogoutRequest request) throws ParseException, JOSEException;

}

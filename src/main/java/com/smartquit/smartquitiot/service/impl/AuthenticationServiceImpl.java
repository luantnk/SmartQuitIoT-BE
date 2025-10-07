package com.smartquit.smartquitiot.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.smartquit.smartquitiot.dto.request.AuthenticationRequest;
import com.smartquit.smartquitiot.dto.request.RefreshTokenRequest;
import com.smartquit.smartquitiot.dto.response.AuthenticationResponse;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.enums.Role;
import com.smartquit.smartquitiot.repository.AccountRepository;
import com.smartquit.smartquitiot.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    @NonFinal
    @Value("${jwt.signerKey}")
    protected String secretKey;
    @NonFinal
    @Value("${refresh.token.key}")
    protected String refreshTokenKey;
    @NonFinal
    @Value("${jwt.validDuration}")
    protected Long accessTokenDuration;
    @NonFinal
    @Value("${jwt.refreshableDuration}")
    protected Long refreshTokenDuration;

    @Override
    public AuthenticationResponse login(AuthenticationRequest request, boolean isSystem) {
        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Username not found"));

        if(!isSystem && !account.getRole().name().equals(Role.MEMBER.name())){
            throw new RuntimeException("Invalid account");
        }else if(isSystem && account.getRole().name().equals(Role.MEMBER.name())){
            throw new RuntimeException("Invalid account");
        }

        if(!account.isActive()){
            throw new RuntimeException("Account is not active");
        }else if(account.isBanned()){
            throw new  RuntimeException("Account is Banned");
        }

        return AuthenticationResponse.builder()
                .accessToken(generateAccessToken(account))
                .refreshToken(generateRefreshToken(account))
                .build();
    }

    private String generateAccessToken(Account account) {

        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(String.valueOf(account.getId()))
                .issuer("SmartQuitIoT")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(accessTokenDuration, ChronoUnit.MINUTES).toEpochMilli()))
                .claim("scope", account.getRole())
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(jwsHeader, payload);
        try{
            jwsObject.sign(new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8)));
            return jwsObject.serialize();
        }catch (JOSEException e){
            throw new RuntimeException(e);
        }
    }

    private String generateRefreshToken(Account account) {
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(String.valueOf(account.getId()))
                .issuer("SmartQuitIoT")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(refreshTokenDuration, ChronoUnit.DAYS).toEpochMilli()))
                .jwtID(String.valueOf(UUID.randomUUID()).substring(0, 8))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(jwsHeader, payload);
        try{
            jwsObject.sign(new MACSigner(refreshTokenKey.getBytes(StandardCharsets.UTF_8)));
            return jwsObject.serialize();
        }catch (JOSEException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest) throws ParseException, JOSEException {

        String accountIdFromSubject = getSubjectFromRefreshToken(refreshTokenRequest.getRefreshToken());
        Date expDate = getExpireDateRefreshToken(refreshTokenRequest.getRefreshToken());
        if(expDate.before(new Date())) {
            throw new JwtException("Refresh token expired");
        }
        Account account = accountRepository.findById(Integer.parseInt(accountIdFromSubject))
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return AuthenticationResponse.builder()
                .accessToken(generateAccessToken(account))
                .refreshToken(generateRefreshToken(account))
                .build();
    }

    private String getSubjectFromRefreshToken(String refreshToken) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(refreshToken);
        JWSVerifier jwsVerifier = new MACVerifier(refreshTokenKey.getBytes());

        boolean verified = signedJWT.verify(jwsVerifier);

        if(!verified){
            throw new JwtException("Invalid refresh token");
        }
        var id = signedJWT.getJWTClaimsSet().getSubject();
        return id.toString();
    }

    private Date getExpireDateRefreshToken(String refreshToken) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(refreshToken);
        JWSVerifier jwsVerifier = new MACVerifier(refreshTokenKey.getBytes());

        boolean verified = signedJWT.verify(jwsVerifier);
        Date expTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        if(!verified){
            throw new JwtException("Invalid refresh token");
        }
        return expTime;
    }
}

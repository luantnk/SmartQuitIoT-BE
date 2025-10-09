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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AccountRepository accountRepository;
    private final AuthenticationManager authenticationManager;

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
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid username/email or password");
        }


        Account account = findAccountByUsernameOrEmail(request.getUsernameOrEmail());

        if (!isSystem && account.getRole() != Role.MEMBER) {
            throw new RuntimeException("Access denied for this account type.");
        } else if (isSystem && account.getRole() == Role.MEMBER) {
            throw new RuntimeException("Member accounts cannot access system resources.");
        }
        if (!account.isActive()) {
            throw new RuntimeException("Account is not active");
        } else if (account.isBanned()) {
            throw new RuntimeException("Account is banned");
        }

        String accessToken = generateAccessToken(account);
        String refreshToken = generateRefreshToken(account);
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private Account findAccountByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail.contains("@")) {
            return accountRepository.findByMemberEmail(usernameOrEmail)
                    .orElseThrow(() -> new RuntimeException("Account for email not found: " + usernameOrEmail));
        } else {
            return accountRepository.findByUsername(usernameOrEmail)
                    .orElseThrow(() -> new RuntimeException("Account for username not found: " + usernameOrEmail));
        }
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) throws ParseException, JOSEException {
        SignedJWT signedJWT = verifyAndParseToken(request.getRefreshToken(), refreshTokenKey);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        if (claims.getExpirationTime().before(new Date())) {
            throw new RuntimeException("Refresh token has expired");
        }
        String accountId = claims.getSubject();
        Account account = accountRepository.findById(Integer.parseInt(accountId))
                .orElseThrow(() -> new RuntimeException("Account not found for this token"));
        String newAccessToken = generateAccessToken(account);
        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .build();
    }

    private String generateAccessToken(Account account) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(String.valueOf(account.getId()))
                .issuer("SmartQuitIoT")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(accessTokenDuration, ChronoUnit.MINUTES).toEpochMilli()))
                .claim("scope", account.getRole().name())
                .claim("isFirstLogin", account.isFirstLogin())
                .build();
        return createSignedJWT(header, claimsSet, secretKey);
    }

    private String generateRefreshToken(Account account) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(String.valueOf(account.getId()))
                .issuer("SmartQuitIoT")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(refreshTokenDuration, ChronoUnit.DAYS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .build();
        return createSignedJWT(header, claimsSet, refreshTokenKey);
    }

    private String createSignedJWT(JWSHeader header, JWTClaimsSet claimsSet, String key) {
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(key.getBytes(StandardCharsets.UTF_8)));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error creating token", e);
        }
    }

    private SignedJWT verifyAndParseToken(String token, String key) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWSVerifier verifier = new MACVerifier(key.getBytes(StandardCharsets.UTF_8));

        if (!signedJWT.verify(verifier)) {
            throw new RuntimeException("Invalid token signature");
        }
        return signedJWT;
    }
}
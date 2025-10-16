package com.smartquit.smartquitiot.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.smartquit.smartquitiot.dto.request.AuthenticationRequest;
import com.smartquit.smartquitiot.dto.request.RefreshTokenRequest;
import com.smartquit.smartquitiot.dto.response.AuthenticationResponse;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.enums.AccountType;
import com.smartquit.smartquitiot.enums.Role;
import com.smartquit.smartquitiot.repository.AccountRepository;
import com.smartquit.smartquitiot.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

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

    @NonFinal
    @Value("${google.client-id}")
    private String googleClientId;



    @Override
    public AuthenticationResponse login(AuthenticationRequest request, boolean isSystem) {
        Account account = accountRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new RuntimeException("Invalid username/email or password"));
        if(!passwordEncoder.matches(request.getPassword(), account.getPassword())){
            throw new RuntimeException("Invalid username/email or password");
        }
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
                .isFirstLogin(account.isFirstLogin())
                .build();
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) throws ParseException, JOSEException {
        SignedJWT signedJWT = verifyAndParseToken(request.getRefreshToken(), refreshTokenKey);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        if (claims.getExpirationTime().before(new Date())) {
            throw new RuntimeException("Refresh token has expired");
        }
        String subject = claims.getSubject(); //Token's subject is account email

        Account account = accountRepository.findByEmail(subject)
                .orElseThrow(() -> new RuntimeException("Account not found for this token"));
        String newAccessToken = generateAccessToken(account);
        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .build();
    }


    @Override
    public AuthenticationResponse loginWithGoogle(String idTokenString) throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new IllegalArgumentException("Invalid ID Token");
        }
        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");
        String pictureUrl = (String) payload.get("picture");
        if (!emailVerified) {
            throw new RuntimeException("Google email not verified");
        }
        Optional<Account> existingUser = accountRepository.findByEmail(email);
        Account account;
        account = existingUser.orElseGet(() -> createNewGoogleAccount(email, firstName, lastName, pictureUrl));
        if (!account.isActive()) {
            throw new RuntimeException("Account is not active");
        }
        if (account.isBanned()) {
            throw new RuntimeException("Account is banned");
        }
        String accessToken = generateAccessToken(account);
        String refreshToken = generateRefreshToken(account);
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isFirstLogin(account.isFirstLogin())
                .build();
    }

    private Account createNewGoogleAccount(String email, String firstName, String lastName, String pictureUrl) {
        Member newMember = new Member();
        newMember.setFirstName(firstName);
        newMember.setLastName(lastName);
        newMember.setAvatarUrl(pictureUrl);
        Account newAccount = new Account();
        newAccount.setEmail(email);
        newAccount.setUsername(email);
        newAccount.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        newAccount.setRole(Role.MEMBER);
        newAccount.setActive(true);
        newAccount.setBanned(false);
        newAccount.setFirstLogin(true);
        newAccount.setAccountType(AccountType.GOOGLE);
        newAccount.setMember(newMember);
        newMember.setAccount(newAccount);
        return accountRepository.save(newAccount);
    }

    private String generateAccessToken(Account account) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(account.getEmail())
                .issuer("SmartQuitIoT")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(1, ChronoUnit.MINUTES).toEpochMilli()))
                .claim("scope", account.getRole().name())
                .claim("username", account.getUsername())
                .build();
        return createSignedJWT(header, claimsSet, secretKey);
    }

    private String generateRefreshToken(Account account) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(account.getEmail())
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
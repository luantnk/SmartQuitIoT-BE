package com.smartquit.smartquitiot.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.smartquit.smartquitiot.dto.request.AuthenticationRequest;
import com.smartquit.smartquitiot.dto.request.RefreshTokenRequest;
import com.smartquit.smartquitiot.dto.request.ResetPasswordRequest;
import com.smartquit.smartquitiot.dto.request.VerifyOtpRequest;
import com.smartquit.smartquitiot.dto.response.AuthenticationResponse;
import com.smartquit.smartquitiot.dto.response.VerifyOtpResponse;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.enums.Role;
import com.smartquit.smartquitiot.repository.AccountRepository;
import com.smartquit.smartquitiot.service.AuthenticationService;
import com.smartquit.smartquitiot.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

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

    private static final long OTP_VALID_DURATION_MINUTES = 5;
    private static final long RESET_TOKEN_VALID_DURATION_MINUTES = 10;

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
    public void forgotPassword(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));
        String otp = String.format("%06d", new Random().nextInt(999999));
        account.setOtp(otp);
        account.setOtpGeneratedTime(LocalDateTime.now());
        accountRepository.save(account);
        String subject = "[SmartQuit] Your Password Reset OTP";
        String username = account.getMember().getFirstName();
        emailService.sendHtmlOtpEmail(email, subject, username, otp);
    }

    @Override
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid OTP or email."));
        if (account.getOtp() == null || !account.getOtp().equals(request.getOtp())) {
            throw new IllegalArgumentException("Invalid OTP. Please try again with another OTP!");
        }
        if (Duration.between(account.getOtpGeneratedTime(), LocalDateTime.now()).toMinutes() > OTP_VALID_DURATION_MINUTES) {
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }
        String token = UUID.randomUUID().toString();
        account.setResetToken(token);
        account.setResetTokenExpiryTime(LocalDateTime.now().plusMinutes(RESET_TOKEN_VALID_DURATION_MINUTES));
        account.setOtp(null);
        account.setOtpGeneratedTime(null);
        accountRepository.save(account);
        return new VerifyOtpResponse(token);
    }



    @Override
    public void resetPassword(ResetPasswordRequest request) {
        Account account = accountRepository.findByResetToken(request.getResetToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token."));
        if (!Duration.between(account.getResetTokenExpiryTime(), LocalDateTime.now()).isNegative()) {
            throw new IllegalArgumentException("Invalid or expired reset token.");
        }
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        account.setResetToken(null);
        account.setResetTokenExpiryTime(null);
        accountRepository.save(account);
    }

    private String generateAccessToken(Account account) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(account.getEmail())
                .issuer("SmartQuitIoT")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(accessTokenDuration, ChronoUnit.MINUTES).toEpochMilli()))
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
package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.*;
import com.smartquit.smartquitiot.dto.response.CoachDTO;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.dto.response.VerifyOtpResponse;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Coach;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.entity.Metric;
import com.smartquit.smartquitiot.enums.AccountType;
import com.smartquit.smartquitiot.enums.Role;
import com.smartquit.smartquitiot.mapper.CoachMapper;
import com.smartquit.smartquitiot.mapper.MemberMapper;
import com.smartquit.smartquitiot.repository.AccountRepository;
import com.smartquit.smartquitiot.repository.CoachRepository;
import com.smartquit.smartquitiot.repository.MemberRepository;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberMapper memberMapper;
    private final CoachRepository coachRepository;
    private final CoachMapper coachMapper;
    private final EmailService emailService;
    @NonFinal
    @Value("${smartquit.default.avatar.url}")
    private String defaultAvatar;

    private static final long OTP_VALID_DURATION_MINUTES = 5;
    private static final long RESET_TOKEN_VALID_DURATION_MINUTES = 10;

    @Transactional
    @Override
    public MemberDTO registerMember(MemberAccountRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Password and confirm password do not match");
        }

        if (accountRepository.findByEmail(request.getEmail()).isPresent()){
            throw new RuntimeException("Email already exists");
        }else if(accountRepository.findByUsername(request.getUsername()).isPresent()){
            throw new RuntimeException("Username already exists");
        }

        Account  account = new Account();
        account.setUsername(request.getUsername());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setAccountType(AccountType.SYSTEM);
        account.setEmail(request.getEmail());
        account.setRole(Role.MEMBER);
        accountRepository.save(account);

        Member member = new Member();
        member.setFirstName(request.getFirstName());
        member.setLastName(request.getLastName());
        member.setGender(request.getGender());
        member.setDob(request.getDob());
        member.setAvatarUrl(getDefaultAvatar(request.getFirstName(), request.getLastName()));
        member.setAccount(account);

        Metric metric = new Metric();
        metric.setStreaks(0);
        metric.setRelapseCountInPhase(0);
        metric.setPost_count(0);
        metric.setComment_count(0);
        metric.setTotal_mission_completed(0);
        metric.setCompleted_all_mission_in_day(0);

        metric.setAvgCravingLevel(0.0);
        metric.setAvgMood(0.0);
        metric.setAvgAnxiety(0.0);
        metric.setAvgConfidentLevel(0.0);
        metric.setAvgCigarettesPerDay(0.0);

        metric.setCurrentCravingLevel(0);
        metric.setCurrentMoodLevel(0);
        metric.setCurrentConfidenceLevel(0);
        metric.setCurrentAnxietyLevel(0);

        metric.setSteps(0);
        metric.setHeartRate(0);
        metric.setSpo2(0);
       // metric.setActivityMinutes(0);
       // metric.setRespiratoryRate(0);
        metric.setSleepDuration(0.0);
       // metric.setSleepQuality(0);

        metric.setAnnualSaved(BigDecimal.ZERO);
        metric.setMoneySaved(BigDecimal.ZERO);

        metric.setReductionPercentage(0.0);
        metric.setSmokeFreeDayPercentage(0.0);
        metric.setMember(member);
        member.setMetric(metric);

        memberRepository.save(member);

        return memberMapper.toMemberDTO(member);
    }

    @Transactional
    @Override
    public CoachDTO registerCoach(CoachAccountRequest request) {

        if(accountRepository.findByUsername(request.getUsername()).isPresent()){
            throw new RuntimeException("Coach username already exists");
        }else if(accountRepository.findByEmail(request.getEmail()).isPresent()){
            throw new RuntimeException("Coach email already exists");
        }else if(!request.getPassword().equals(request.getConfirmPassword())){
            throw new RuntimeException("Password and confirm password do not match");
        }

        Account  account = new Account();
        account.setUsername(request.getUsername());
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setAccountType(AccountType.SYSTEM);
        account.setFirstLogin(false);
        account.setRole(Role.COACH);
        accountRepository.save(account);

        Coach coach = new Coach();
        coach.setFirstName(request.getFirstName());
        coach.setLastName(request.getLastName());
        coach.setGender(request.getGender());
        coach.setCertificateUrl(request.getCertificateUrl());
        coach.setExperienceYears(request.getExperienceYears());
        coach.setSpecializations(request.getSpecializations());
        coach.setAvatarUrl(getDefaultAvatar(request.getFirstName(), request.getLastName()));
        coach.setAccount(account);
        coachRepository.save(coach);

        return coachMapper.toCoachDTO(coach);
    }

    @Override
    public Account getAuthenticatedAccount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        String subject = auth.getName();
        return accountRepository.findByUsernameOrEmail(subject, subject)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }


    private String getDefaultAvatar(String firstName, String lastName){
        defaultAvatar += firstName+ "+" + lastName;
        return defaultAvatar;
    }

    @Override
    public void updatePassword(ChangePasswordRequest request) {
        Account account = getAuthenticatedAccount();
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String newPasswordConfirm = request.getNewPasswordConfirm();
        if(!passwordEncoder.matches(oldPassword, account.getPassword())){
            throw new RuntimeException("Incorrect old password");
        }
        if(!newPassword.equals(newPasswordConfirm)){
            throw new RuntimeException("Incorrect confirm password");
        }
        account.setPassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
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
}

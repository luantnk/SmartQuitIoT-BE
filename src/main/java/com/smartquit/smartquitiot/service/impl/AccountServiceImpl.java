package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.CoachAccountRequest;
import com.smartquit.smartquitiot.dto.request.MemberAccountRequest;
import com.smartquit.smartquitiot.dto.response.CoachDTO;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Coach;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.enums.AccountType;
import com.smartquit.smartquitiot.enums.Role;
import com.smartquit.smartquitiot.mapper.CoachMapper;
import com.smartquit.smartquitiot.mapper.MemberMapper;
import com.smartquit.smartquitiot.repository.AccountRepository;
import com.smartquit.smartquitiot.repository.CoachRepository;
import com.smartquit.smartquitiot.repository.MemberRepository;
import com.smartquit.smartquitiot.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Local;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberMapper memberMapper;
    private final CoachRepository coachRepository;
    private final CoachMapper coachMapper;
    @NonFinal
    @Value("${smartquit.default.avatar.url}")
    private String defaultAvatar;

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
        String subject = SecurityContextHolder.getContext().getAuthentication().getName();//Subject is account email
        return accountRepository.findByEmail(subject).orElseThrow(() -> new RuntimeException("Account not found"));
    }

    private String getDefaultAvatar(String firstName, String lastName){
        defaultAvatar += firstName+ "+" + lastName;
        return defaultAvatar;
    }

}

package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.repository.MemberRepository;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final AccountService accountService;
    private final MemberRepository memberRepository;

    @Override
    public Member getAuthenticatedMember() {
        Account authAccount = accountService.getAuthenticatedAccount();
        return memberRepository.findByAccountId(authAccount.getId()).orElseThrow(() -> new RuntimeException("Member not found"));
    }
}

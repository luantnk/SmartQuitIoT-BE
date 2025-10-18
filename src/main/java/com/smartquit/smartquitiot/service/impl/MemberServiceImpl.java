package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.MemberUpdateRequest;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.mapper.MemberMapper;
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
    private final MemberMapper memberMapper;

    @Override
    public Member getAuthenticatedMember() {
        Account authAccount = accountService.getAuthenticatedAccount();
        return memberRepository.findByAccountId(authAccount.getId()).orElseThrow(() -> new RuntimeException("Member not found"));
    }

    @Override
    public MemberDTO getAuthenticatedMemberProfile() {
        Member member = getAuthenticatedMember();
        return memberMapper.toMemberDTO(member);
    }

    @Override
    public MemberDTO getMemberById(int id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Member not found"));
        return memberMapper.toMemberDTO(member);
    }

    @Override
    public MemberDTO updateProfile(MemberUpdateRequest request) {
        Member member = getAuthenticatedMember();
        member.setFirstName(request.getFirstName());
        member.setLastName(request.getLastName());
        if(request.getAvatarUrl() != null) {
            member.setAvatarUrl(request.getAvatarUrl());
        }
        if(request.getDob() != null){
            member.setDob(request.getDob());
        }
        member = memberRepository.save(member);
        return memberMapper.toMemberDTO(member);
    }
}

package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.MemberReminderSettingsRequest;
import com.smartquit.smartquitiot.dto.request.MemberUpdateRequest;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.dto.response.MemberListItemDTO;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.mapper.MemberMapper;
import com.smartquit.smartquitiot.repository.MemberRepository;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.MemberService;
import com.smartquit.smartquitiot.specifications.MemberSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public Page<MemberDTO> getMembers(int page, int size, String search, Boolean isActive) {
        Specification<Member> spec = Specification.allOf(MemberSpecification.hasSearchString(search).and(MemberSpecification.hasActive(isActive)));
        Pageable pageable = PageRequest.of(page, size);
        Page<Member> memberPage = memberRepository.findAll(spec, pageable);
        return memberPage.map(memberMapper::toMemberDTO);
    }

    @Override
    public List<MemberListItemDTO> getListMembers() {
        List<Member> all = memberRepository.findAllByAccount_IsActiveTrueAndAccount_IsBannedFalse();
        return all.stream().map(memberMapper::toMemberListItemDTO).collect(Collectors.toList());
    }

    @Override
    public MemberDTO updateReminderSettings(MemberReminderSettingsRequest req) {
        Member member = accountService.getAuthenticatedAccount().getMember();

        member.setMorningReminderTime(req.getMorningReminderTime());
        member.setQuietStart(req.getQuietStart());
        member.setQuietEnd(req.getQuietEnd());

        memberRepository.save(member);
        return  memberMapper.toMemberDTO(member);
    }
}

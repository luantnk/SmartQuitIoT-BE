package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberMapper {

    private final AccountMapper accountMapper;

    public MemberDTO toMemberDTO(Member member) {
        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setId(member.getId());
        memberDTO.setEmail(member.getEmail());
        memberDTO.setFirstName(member.getFirstName());
        memberDTO.setLastName(member.getLastName());
        memberDTO.setGender(member.getGender());
        memberDTO.setDob(member.getDob());
        memberDTO.setAvatarUrl(member.getAvatarUrl());
        memberDTO.setUsedFreeTrial(member.isUsedFreeTrial());
        memberDTO.setAge(member.getAge());
        memberDTO.setAccount(accountMapper.toAccountDTO(member.getAccount()));

        return memberDTO;
    }
}

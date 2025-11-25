package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.dto.response.MemberListItemDTO;
import com.smartquit.smartquitiot.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

@Component
@RequiredArgsConstructor
public class MemberMapper {

    private final AccountMapper accountMapper;

    public MemberDTO toMemberDTO(Member member) {
        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setId(member.getId());
        memberDTO.setFirstName(member.getFirstName());
        memberDTO.setLastName(member.getLastName());
        memberDTO.setGender(member.getGender());
        memberDTO.setDob(member.getDob());
        memberDTO.setAvatarUrl(member.getAvatarUrl());
        memberDTO.setIsUsedFreeTrial(member.isUsedFreeTrial());
        memberDTO.setAge(calculateAge(member.getDob()));
        memberDTO.setAccount(accountMapper.toAccountDTO(member.getAccount()));
        memberDTO.setMorningReminderTime(member.getMorningReminderTime());
        memberDTO.setQuietStart(member.getQuietStart());
        memberDTO.setQuietEnd(member.getQuietEnd());
        memberDTO.setTimeZone(member.getTimeZone());
        return memberDTO;
    }

    private Integer calculateAge(LocalDate dob){
        if(dob == null) return null;
        return Period.between(dob, LocalDate.now()).getYears();
    }
    public MemberListItemDTO toMemberListItemDTO(Member member) {
        MemberListItemDTO memberListItemDTO = new MemberListItemDTO();
        memberListItemDTO.setId(member.getId());
        memberListItemDTO.setFirstName(member.getFirstName());
        memberListItemDTO.setLastName(member.getLastName());
        memberListItemDTO.setGender(member.getGender());
        memberListItemDTO.setDob(member.getDob());
        memberListItemDTO.setAge(calculateAge(member.getDob()));
        memberListItemDTO.setAvatarUrl(member.getAvatarUrl());
        memberListItemDTO.setUsedFreeTrial(member.isUsedFreeTrial());

        if (member.getMetric() != null) {
            memberListItemDTO.setStreaks(member.getMetric().getStreaks());
            memberListItemDTO.setReductionPercentage(member.getMetric().getReductionPercentage());
            memberListItemDTO.setSmokeFreeDayPercentage(member.getMetric().getSmokeFreeDayPercentage());
        } else {
            memberListItemDTO.setStreaks(0);
            memberListItemDTO.setReductionPercentage(0.0);
            memberListItemDTO.setSmokeFreeDayPercentage(0.0);
        }

        return memberListItemDTO;
    }

    public MemberDTO toMemberPayment(Member member) {
        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setId(member.getId());
        memberDTO.setFirstName(member.getFirstName());
        memberDTO.setLastName(member.getLastName());
        memberDTO.setGender(member.getGender());
        return memberDTO;
    }
}

package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.CoachDTO;
import com.smartquit.smartquitiot.entity.Coach;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoachMapper {

    private final AccountMapper accountMapper;

    public CoachDTO toCoachDTO(Coach coach) {

        CoachDTO coachDTO = new CoachDTO();
        coachDTO.setId(coach.getId());
        coachDTO.setFirstName(coach.getFirstName());
        coachDTO.setLastName(coach.getLastName());
        coachDTO.setAvatarUrl(coach.getAvatarUrl());
        coachDTO.setGender(coach.getGender().name());
        coachDTO.setRatingCount(coach.getRatingCount());
        coachDTO.setRatingAvg(coach.getRatingAvg());
        coachDTO.setBio(coach.getBio());
        coachDTO.setCertificateUrl(coach.getCertificateUrl());
        coachDTO.setAccount(accountMapper.toAccountDTO(coach.getAccount()));

        return coachDTO;
    }
}

package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.MissionDTO;
import com.smartquit.smartquitiot.entity.Mission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MissionMapper {

    private final MissionTypeMapper missionTypeMapper;
    private final InterestCategoryMapper interestCategoryMapper;

    public MissionDTO toMissionDTO(Mission mission){
        if(mission == null) return null;
        MissionDTO dto = new MissionDTO();
        dto.setId(mission.getId());
        dto.setCode(mission.getCode());
        dto.setName(mission.getName());
        dto.setDescription(mission.getDescription());
        dto.setCondition(mission.getCondition());
        dto.setPhase(mission.getPhase().name());
        dto.setStatus(mission.getStatus().name());
        dto.setExp(mission.getExp());
        dto.setCreatedAt(mission.getCreatedAt());
        dto.setUpdatedAt(mission.getUpdatedAt());
        dto.setMissionType(missionTypeMapper.toMissionTypeDTO(mission.getMissionType()));
        dto.setInterestCategory(interestCategoryMapper.toInterestCategoryDTO(mission.getInterestCategory()));
        return dto;
    }
}

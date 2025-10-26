package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.MissionTypeDTO;
import com.smartquit.smartquitiot.entity.MissionType;
import org.springframework.stereotype.Component;

@Component
public class MissionTypeMapper {

    public MissionTypeDTO toMissionTypeDTO(MissionType missionType){
        if(missionType == null) return null;
        MissionTypeDTO dto = new MissionTypeDTO();
        dto.setId(missionType.getId());
        dto.setName(missionType.getName());
        dto.setDescription(missionType.getDescription());
        return dto;
    }
}

package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.MissionTypeDTO;
import com.smartquit.smartquitiot.entity.MissionType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<MissionTypeDTO> toListDTO(List<MissionType> missionTypeList){
        if (missionTypeList == null) return Collections.emptyList();
        return missionTypeList.stream().map(this::toMissionTypeDTO).collect(Collectors.toList());
    }
}

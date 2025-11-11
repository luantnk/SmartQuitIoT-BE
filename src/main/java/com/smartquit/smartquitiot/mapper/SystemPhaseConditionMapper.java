package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.SystemPhaseConditionDTO;
import com.smartquit.smartquitiot.entity.SystemPhaseCondition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SystemPhaseConditionMapper {


    public List<SystemPhaseConditionDTO> toListDTO (List<SystemPhaseCondition> systemPhaseConditions) {

        return systemPhaseConditions.stream().map( condition ->{
            SystemPhaseConditionDTO dto = new SystemPhaseConditionDTO();
            dto.setId(condition.getId());
            dto.setName(condition.getName());
            dto.setCondition(condition.getCondition());
            dto.setUpdatedAt(condition.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());

    }

    public SystemPhaseConditionDTO toDTO (SystemPhaseCondition systemPhaseCondition) {
        SystemPhaseConditionDTO dto = new SystemPhaseConditionDTO();
        dto.setId(systemPhaseCondition.getId());
        dto.setName(systemPhaseCondition.getName());
        dto.setCondition(systemPhaseCondition.getCondition());
        dto.setUpdatedAt(systemPhaseCondition.getUpdatedAt());
        return dto;
    }

}

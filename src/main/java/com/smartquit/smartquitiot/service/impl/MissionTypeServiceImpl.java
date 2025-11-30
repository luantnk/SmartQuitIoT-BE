package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.MissionTypeDTO;
import com.smartquit.smartquitiot.entity.InterestCategory;
import com.smartquit.smartquitiot.entity.MissionType;
import com.smartquit.smartquitiot.mapper.MissionTypeMapper;
import com.smartquit.smartquitiot.repository.MissionTypeRepository;
import com.smartquit.smartquitiot.service.MissionTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissionTypeServiceImpl implements MissionTypeService {
    private final MissionTypeMapper missionTypeMapper;
    private final MissionTypeRepository missionTypeRepository;
    @Override
    public List<MissionTypeDTO> getAllMissionTypes() {
        List<MissionType> missionTypes = missionTypeRepository.findAll();
        if(missionTypes.isEmpty()){
            throw new IllegalArgumentException("Mission Types List is Empty");
        }
        return  missionTypeMapper.toListDTO(missionTypes);
    }
}

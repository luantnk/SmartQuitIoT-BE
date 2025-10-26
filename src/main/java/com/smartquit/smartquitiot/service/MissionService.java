package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.response.MissionDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.enums.MissionStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MissionService {
    List<Mission> filterMissionsForPhase(
             QuitPlan plan, Account account,
            MissionPhase missionPhase, MissionStatus missionStatus
    );

    Page<MissionDTO> getAllMissions(int page, int size);
}

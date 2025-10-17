package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.enums.MissionStatus;

import java.util.List;

public interface MissionService {
    List<Mission> filterMissionsForPhase(
             QuitPlan plan, Account account,
            MissionPhase missionPhase, MissionStatus missionStatus
    );
}

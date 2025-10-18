package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.response.PhaseBatchMissionsResponse;
import com.smartquit.smartquitiot.entity.PhaseDetail;
import com.smartquit.smartquitiot.entity.QuitPlan;
import com.smartquit.smartquitiot.enums.MissionPhase;

import java.util.List;

public interface PhaseDetailMissionService {
    int savePhaseDetailMissionsForPhase(PhaseBatchMissionsResponse resp);
    PhaseBatchMissionsResponse generatePhaseDetailMissionsForPhase(List<PhaseDetail> preparedDetails , QuitPlan plan,
                                                                   int maxPerDay, String phaseName, MissionPhase missionPhase);
}

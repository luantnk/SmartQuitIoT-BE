package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.CompleteMissionRequest;
import com.smartquit.smartquitiot.dto.response.*;
import com.smartquit.smartquitiot.entity.Phase;
import com.smartquit.smartquitiot.entity.PhaseDetail;
import com.smartquit.smartquitiot.entity.QuitPlan;
import com.smartquit.smartquitiot.enums.MissionPhase;

import java.util.List;

public interface PhaseDetailMissionService {
    int savePhaseDetailMissionsForPhase(PhaseBatchMissionsResponse resp);
    PhaseBatchMissionsResponse generatePhaseDetailMissionsForPhase(List<PhaseDetail> preparedDetails , QuitPlan plan,
                                                                   int maxPerDay, String phaseName, MissionPhase missionPhase);

    QuitPlanResponse completePhaseDetailMission(CompleteMissionRequest completeMissionRequest);

    MissionTodayResponse getListMissionToday();
    MissionTodayResponse completePhaseDetailMissionAtHomePage(CompleteMissionRequest completeMissionRequest);
    PhaseBatchMissionsResponse generatePhaseDetailMissionsForPhaseInScheduler(Phase phase, List<PhaseDetail> preparedDetails , QuitPlan plan, int maxPerDay, String phaseName, MissionPhase missionPhase);

    MissionsInPhaseWrapperResponse  getAllMissionsInCurrentPhaseByMemberId(int memberId);
}

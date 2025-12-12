package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.entity.Phase;
import com.smartquit.smartquitiot.entity.PhaseDetail;
import com.smartquit.smartquitiot.entity.QuitPlan;

import java.util.List;

public interface PhaseDetailService {
    List<PhaseDetail> generatePhaseDetailsForPhase(Phase phase);
    List<PhaseDetail> generateInitialPhaseDetails(QuitPlan quitPlan, String phaseName);
}

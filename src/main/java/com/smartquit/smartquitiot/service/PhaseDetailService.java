package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.entity.Phase;
import com.smartquit.smartquitiot.entity.QuitPlan;

public interface PhaseDetailService {
     void generatePhaseDetailsForPhase(Phase phase);
     void generateInitialPhaseDetails(QuitPlan quitPlan);
}

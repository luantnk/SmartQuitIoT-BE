package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.CreateQuitPlanInFirstLoginRequest;
import com.smartquit.smartquitiot.dto.request.RedoPhaseRequest;
import com.smartquit.smartquitiot.dto.response.PhaseDTO;
import com.smartquit.smartquitiot.dto.response.PhaseResponse;
import com.smartquit.smartquitiot.dto.response.QuitPlanResponse;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.QuitPlan;

import java.time.LocalDate;

public interface PhaseService {
    PhaseResponse generatePhasesInFirstLogin(CreateQuitPlanInFirstLoginRequest req, int FTND, Account account);
    void savePhasesAndSystemPhaseCondition(PhaseResponse phaseResponse, QuitPlan quitPlan);
    PhaseDTO getCurrentPhaseAtHomePage();
    void updateQuitPlanAndPhaseStatuses();
    PhaseResponse generatePhases(int smokeAvgPerDay, int numberOfYearsSmoking, LocalDate startDate, int FTND, Account account);
    QuitPlanResponse redoPhaseInFailed(RedoPhaseRequest redoPhaseRequest);
}

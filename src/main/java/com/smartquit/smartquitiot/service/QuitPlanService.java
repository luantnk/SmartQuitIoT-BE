package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.CreateQuitPlanInFirstLoginRequest;
import com.smartquit.smartquitiot.dto.response.PhaseBatchMissionsResponse;
import com.smartquit.smartquitiot.dto.response.QuitPlanResponse;
import com.smartquit.smartquitiot.dto.response.TimeResponse;

public interface QuitPlanService {
     PhaseBatchMissionsResponse createQuitPlanInFirstLogin(CreateQuitPlanInFirstLoginRequest req);
     QuitPlanResponse getCurrentQuitPlan();
     TimeResponse getCurrentTimeOfQuitPlan();
    QuitPlanResponse getMemberQuitPlan(int memberId);
 }

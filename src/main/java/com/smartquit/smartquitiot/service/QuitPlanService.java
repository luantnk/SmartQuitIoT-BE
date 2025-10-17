package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.CreateQuitPlanInFirstLoginRequest;
import com.smartquit.smartquitiot.dto.response.PhaseBatchMissionsResponse;
import com.smartquit.smartquitiot.dto.response.QuitPlanResponse;

public interface QuitPlanService {
     PhaseBatchMissionsResponse createQuitPlanInFirstLogin(CreateQuitPlanInFirstLoginRequest req);
     QuitPlanResponse getCurrentQuitPlan();
}

package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.TestConditionRequest;
import com.smartquit.smartquitiot.dto.request.UpdateSystemPhaseConditionRequest;
import com.smartquit.smartquitiot.dto.response.SystemPhaseConditionDTO;
import com.smartquit.smartquitiot.dto.response.TestConditionResponse;

import java.util.List;

public interface SystemPhaseConditionService {
    List<SystemPhaseConditionDTO> getAllSystemPhaseCondition();
    SystemPhaseConditionDTO updateSystemPhaseCondition(Integer id,UpdateSystemPhaseConditionRequest req);
    TestConditionResponse testCondition(TestConditionRequest request);
}

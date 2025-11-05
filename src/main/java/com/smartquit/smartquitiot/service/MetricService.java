package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.response.MetricDTO;

import java.util.Map;

public interface MetricService {

    Map<String, Object> getHomeScreenMetrics();
    Map<String, Object> getHealthMetrics();
    Map<String, Object> getHomeScreenHealthRecoveryMetrics();
    Map<String, Object> getHealthMetricsByMemberId(int memberId);
}

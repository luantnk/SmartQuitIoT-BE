package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.response.MetricDTO;

import java.util.Map;

public interface MetricService {

    MetricDTO getHomeScreenMetrics();
    Map<String, Object> getHealthMetrics();
}

package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.MetricDTO;
import com.smartquit.smartquitiot.entity.HealthRecovery;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.entity.Metric;
import com.smartquit.smartquitiot.mapper.MetricMapper;
import com.smartquit.smartquitiot.repository.HealthRecoveryRepository;
import com.smartquit.smartquitiot.repository.MetricRepository;
import com.smartquit.smartquitiot.service.MemberService;
import com.smartquit.smartquitiot.service.MetricService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MetricServiceImpl implements MetricService {
    private final MemberService memberService;
    private final MetricRepository metricRepository;
    private final HealthRecoveryRepository healthRecoveryRepository;
    private final MetricMapper metricMapper;

    @Override
    public MetricDTO getHomeScreenMetrics() {
        Member member = memberService.getAuthenticatedMember();
        Metric metric = metricRepository.findByMemberId(member.getId()).orElse(null);
        return metricMapper.toMetricStatistic(metric);
    }

    @Override
    public Map<String, Object> getHealthMetrics() {
        Map<String, Object> response = new HashMap<>();
        Member member = memberService.getAuthenticatedMember();
        Metric metric = metricRepository.findByMemberId(member.getId()).orElse(null);
        List<HealthRecovery> healthRecoveries = healthRecoveryRepository.findByMemberId(member.getId());
        response.put("metrics", metric);
        response.put("healthRecoveries", healthRecoveries);
        return response;
    }
}

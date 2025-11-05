package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.MetricDTO;
import com.smartquit.smartquitiot.entity.DiaryRecord;
import com.smartquit.smartquitiot.entity.HealthRecovery;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.entity.Metric;
import com.smartquit.smartquitiot.enums.HealthRecoveryDataName;
import com.smartquit.smartquitiot.mapper.MetricMapper;
import com.smartquit.smartquitiot.repository.DiaryRecordRepository;
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
    private final DiaryRecordRepository diaryRecordRepository;

    @Override
    public Map<String, Object> getHomeScreenMetrics() {
        Map<String, Object> response = new HashMap<>();
        Member member = memberService.getAuthenticatedMember();
        Metric metric = metricRepository.findByMemberId(member.getId()).orElse(null);
        MetricDTO metricDTO = metricMapper.toMetricStatistic(metric);
        List<DiaryRecord> records = diaryRecordRepository.findByMemberId(member.getId());
        response.put("metric", metricDTO);
        response.put("cravingLevelChart", records.stream().map(record -> Map.of(
                "date", record.getDate(),
                "cravingLevel", record.getCravingLevel()
        )).toList());
        return response;
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

    @Override
    public Map<String, Object> getHomeScreenHealthRecoveryMetrics() {
        Map<String, Object> response = new HashMap<>();
        Member member = memberService.getAuthenticatedMember();
        HealthRecovery pulseRateRecovery = healthRecoveryRepository.findByNameAndMemberId(HealthRecoveryDataName.PULSE_RATE, member.getId()).orElse(null);
        HealthRecovery oxygenLevelRecovery = healthRecoveryRepository.findByNameAndMemberId(HealthRecoveryDataName.OXYGEN_LEVEL, member.getId()).orElse(null);
        HealthRecovery carbonMonoxideRecovery = healthRecoveryRepository.findByNameAndMemberId(HealthRecoveryDataName.CARBON_MONOXIDE_LEVEL, member.getId()).orElse(null);
        response.put("pulseRate", pulseRateRecovery);
        response.put("oxygenLevel", oxygenLevelRecovery);
        response.put("carbonMonoxideLevel", carbonMonoxideRecovery);
        return response;
    }

    @Override
    public Map<String, Object> getHealthMetricsByMemberId(int memberId) {
        Map<String, Object> response = new HashMap<>();
        Metric metric = metricRepository.findByMemberId(memberId).orElse(null);
        List<HealthRecovery> healthRecoveries = healthRecoveryRepository.findByMemberId(memberId);
        response.put("metrics", metric);
        response.put("healthRecoveries", healthRecoveries);
        return response;
    }
}

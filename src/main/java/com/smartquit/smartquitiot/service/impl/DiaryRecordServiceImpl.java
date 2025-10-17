package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.DiaryRecordRequest;
import com.smartquit.smartquitiot.dto.response.DiaryRecordDTO;
import com.smartquit.smartquitiot.entity.DiaryRecord;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.entity.Metric;
import com.smartquit.smartquitiot.mapper.DiaryRecordMapper;
import com.smartquit.smartquitiot.repository.DiaryRecordRepository;
import com.smartquit.smartquitiot.repository.MetricRepository;
import com.smartquit.smartquitiot.repository.QuitPlanRepository;
import com.smartquit.smartquitiot.service.DiaryRecordService;
import com.smartquit.smartquitiot.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiaryRecordServiceImpl implements DiaryRecordService {

    private final DiaryRecordMapper diaryRecordMapper;
    private final DiaryRecordRepository diaryRecordRepository;
    private final MemberService memberService;
    private final MetricRepository metricRepository;
    private final QuitPlanRepository quitPlanRepository;

    @Override
    public DiaryRecordDTO logDiaryRecord(DiaryRecordRequest request) {
        Member member = memberService.getAuthenticatedMember();

        Optional<DiaryRecord> isExistingTodayRecord = diaryRecordRepository.findByDateAndMemberId(request.getDate(), member.getId());
        if (isExistingTodayRecord.isPresent()) {
            throw new RuntimeException("You have been enter today record");
        }

        DiaryRecord diaryRecord = new DiaryRecord();
        diaryRecord.setDate(request.getDate());
        diaryRecord.setHaveSmoked(request.isHaveSmoked());
        //sau này sẽ xử lí nếu hút trong quá trình cai thuốc sau
        diaryRecord.setCigarettesSmoked(request.getCigarettesSmoked());
        diaryRecord.setTriggers(request.getTriggers());
        diaryRecord.setUseNrt(request.isUseNrt());
        diaryRecord.setMoneySpentOnNrt(request.getMoneySpentOnNrt());
        diaryRecord.setCravingLevel(request.getCravingLevel());
        diaryRecord.setMoodLevel(request.getMoodLevel());
        diaryRecord.setConfidenceLevel(request.getConfidenceLevel());
        diaryRecord.setAnxietyLevel(request.getAnxietyLevel());
        diaryRecord.setNote(request.getNote());
        diaryRecord.setConnectIoTDevice(request.isConnectIoTDevice());
        if(request.isConnectIoTDevice()){
            diaryRecord.setSteps(request.getSteps());
            diaryRecord.setHeartRate(request.getHeartRate());
            diaryRecord.setSpo2(request.getSpo2());
            diaryRecord.setActivityMinutes(request.getActivityMinutes());
            diaryRecord.setRespiratoryRate(request.getRespiratoryRate());
            diaryRecord.setSleepDuration(request.getSleepDuration());
            diaryRecord.setSleepQuality(request.getSleepQuality());
        }
        diaryRecord.setMember(member);
        diaryRecord = diaryRecordRepository.save(diaryRecord);

        Metric metric = metricRepository.findByMemberId(member.getId()).orElseGet(
                () -> {
                    Metric newMetric = new Metric();
                    newMetric.setMember(member);
                    newMetric.setStreaks(1);
                    newMetric.setRelapseCountInPhase(0);
                    newMetric.setAvgCravingLevel(request.getCravingLevel());
                    newMetric.setAvgMood(request.getMoodLevel());
                    newMetric.setAvgAnxiety(request.getAnxietyLevel());
                    newMetric.setAvgConfidentLevel(request.getConfidenceLevel());
                    newMetric.setCurrentAnxietyLevel(request.getAnxietyLevel());
                    newMetric.setCurrentCravingLevel(request.getCravingLevel());
                    newMetric.setCurrentConfidenceLevel(request.getConfidenceLevel());
                    newMetric.setCurrentMoodLevel(request.getMoodLevel());

                    return newMetric;
                }
        );

        return diaryRecordMapper.toDiaryRecordDTO(diaryRecord);
    }
}

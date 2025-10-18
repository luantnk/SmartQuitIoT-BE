package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.DiaryRecordRequest;
import com.smartquit.smartquitiot.dto.response.DiaryRecordDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.mapper.DiaryRecordMapper;
import com.smartquit.smartquitiot.repository.DiaryRecordRepository;
import com.smartquit.smartquitiot.repository.MetricRepository;
import com.smartquit.smartquitiot.repository.QuitPlanRepository;
import com.smartquit.smartquitiot.service.DiaryRecordService;
import com.smartquit.smartquitiot.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiaryRecordServiceImpl implements DiaryRecordService {

    private final DiaryRecordMapper diaryRecordMapper;
    private final DiaryRecordRepository diaryRecordRepository;
    private final MemberService memberService;
    private final MetricRepository metricRepository;
    private final QuitPlanRepository quitPlanRepository;

    @Transactional
    @Override
    public DiaryRecordDTO logDiaryRecord(DiaryRecordRequest request) {
        Member member = memberService.getAuthenticatedMember();
        QuitPlan currentQuitPlan = quitPlanRepository.findTopByMemberIdOrderByCreatedAtDesc(member.getId());
        FormMetric currentFormMetric = currentQuitPlan.getFormMetric();

        Optional<DiaryRecord> isExistingTodayRecord = diaryRecordRepository.findByDateAndMemberId(request.getDate(), member.getId());
        if (isExistingTodayRecord.isPresent()) {
            throw new RuntimeException("You have been enter today record");
        }

        if(request.getDate().isAfter(LocalDate.now())){
            throw new RuntimeException("You can not enter record in the future day");
        }

        BigDecimal amountNicotinePerCigarettesOfMemberForm = currentFormMetric.getAmountOfNicotinePerCigarettes();
        BigDecimal estimateNicotineIntake = amountNicotinePerCigarettesOfMemberForm.multiply(BigDecimal.valueOf(request.getCigarettesSmoked()));

        DiaryRecord diaryRecord = new DiaryRecord();
        diaryRecord.setDate(request.getDate());
        diaryRecord.setHaveSmoked(request.getHaveSmoked());
        //sau này sẽ xử lí nếu hút trong quá trình cai thuốc sau
        diaryRecord.setCigarettesSmoked(request.getCigarettesSmoked());
        diaryRecord.setTriggers(request.getTriggers());
        diaryRecord.setUseNrt(request.getIsUseNrt());
        diaryRecord.setMoneySpentOnNrt(request.getMoneySpentOnNrt());
        diaryRecord.setCravingLevel(request.getCravingLevel());
        diaryRecord.setMoodLevel(request.getMoodLevel());
        diaryRecord.setConfidenceLevel(request.getConfidenceLevel());
        diaryRecord.setAnxietyLevel(request.getAnxietyLevel());
        diaryRecord.setNote(request.getNote());
        diaryRecord.setEstimatedNicotineIntake(estimateNicotineIntake);
        diaryRecord.setConnectIoTDevice(request.getIsConnectIoTDevice());
        if(request.getIsConnectIoTDevice()){
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
                    //init metric if this is the first time member log diary record.
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

                    if(request.getSteps() != null){
                        newMetric.setSteps(request.getSteps());
                    }
                    if(request.getHeartRate() != null){
                        newMetric.setHeartRate(request.getHeartRate());
                    }
                    if(request.getSpo2() != null){
                        newMetric.setSpo2(request.getSpo2());
                    }
                    if(request.getActivityMinutes() != null){
                        newMetric.setActivityMinutes(request.getActivityMinutes());
                    }
                    if(request.getRespiratoryRate() != null){
                        newMetric.setRespiratoryRate(request.getRespiratoryRate());
                    }
                    if(request.getSleepDuration() != null){
                        newMetric.setSleepDuration(request.getSleepDuration());
                    }
                    if(request.getSleepQuality() != null){
                        newMetric.setSleepQuality(request.getSleepQuality());
                    }

                    return metricRepository.save(newMetric);
                }
        );

        int streaksCount = 1;
        Optional<DiaryRecord> previousDayRecord = diaryRecordRepository.findTopByMemberIdOrderByDate(member.getId());
        if(previousDayRecord.isPresent()){
            LocalDate date = previousDayRecord.get().getDate();
            LocalDate yesterday = LocalDate.now().minusDays(1);
            boolean isYesterday = date.isEqual(yesterday);
            if(isYesterday){
                streaksCount++;
            }
        }

        List<DiaryRecord> records = diaryRecordRepository.findByMemberId(member.getId());
        int count = records.size(); // number of member's diary records
        double newAvgCravingLevel = metric.getAvgCravingLevel() + (request.getCravingLevel() - metric.getAvgCravingLevel()) / (count +1);
        double newAvgMoodLevel = metric.getAvgMood() + (request.getMoodLevel() - metric.getAvgMood()) / (count +1);
        double newAvgConfidenceLevel = metric.getAvgConfidentLevel() + (request.getConfidenceLevel() - metric.getAvgConfidentLevel()) / (count +1);
        double newAvgAnxietyLevel = metric.getAvgAnxiety() + (request.getAnxietyLevel() - metric.getAvgAnxiety()) / (count +1);

        metric.setStreaks(streaksCount);
        metric.setAvgCravingLevel(newAvgCravingLevel);
        metric.setAvgMood(newAvgMoodLevel);
        metric.setAvgConfidentLevel(newAvgConfidenceLevel);
        metric.setAvgAnxiety(newAvgAnxietyLevel);
        metric.setCurrentAnxietyLevel(request.getAnxietyLevel());
        metric.setCurrentCravingLevel(request.getCravingLevel());
        metric.setCurrentConfidenceLevel(request.getConfidenceLevel());
        metric.setCurrentMoodLevel(request.getMoodLevel());
        if(request.getSteps() != null){
            metric.setSteps(request.getSteps());
        }
        if(request.getHeartRate() != null){
            metric.setHeartRate(request.getHeartRate());
        }
        if(request.getSpo2() != null){
            metric.setSpo2(request.getSpo2());
        }
        if(request.getActivityMinutes() != null){
            metric.setActivityMinutes(request.getActivityMinutes());
        }
        if(request.getRespiratoryRate() != null){
            metric.setRespiratoryRate(request.getRespiratoryRate());
        }
        if(request.getSleepDuration() != null){
            metric.setSleepDuration(request.getSleepDuration());
        }
        if(request.getSleepQuality() != null){
            metric.setSleepQuality(request.getSleepQuality());
        }
        metricRepository.save(metric);

        return diaryRecordMapper.toDiaryRecordDTO(diaryRecord);
    }
}

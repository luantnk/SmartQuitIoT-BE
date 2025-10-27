package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.DiaryRecordRequest;
import com.smartquit.smartquitiot.dto.response.DiaryRecordDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.HealthRecoveryDataName;
import com.smartquit.smartquitiot.mapper.DiaryRecordMapper;
import com.smartquit.smartquitiot.repository.DiaryRecordRepository;
import com.smartquit.smartquitiot.repository.HealthRecoveryRepository;
import com.smartquit.smartquitiot.repository.MetricRepository;
import com.smartquit.smartquitiot.repository.QuitPlanRepository;
import com.smartquit.smartquitiot.service.DiaryRecordService;
import com.smartquit.smartquitiot.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DiaryRecordServiceImpl implements DiaryRecordService {

    private final DiaryRecordMapper diaryRecordMapper;
    private final DiaryRecordRepository diaryRecordRepository;
    private final MemberService memberService;
    private final MetricRepository metricRepository;
    private final QuitPlanRepository quitPlanRepository;
    private final HealthRecoveryRepository healthRecoveryRepository;

    private final int PULSE_RATE_TO_NORMAL = 20; //minutes
    private final int OXYGEN_LEVEL_TO_NORMAL = 480; //minutes => 8 hours
    private final int CARBON_MONOXIDE_TO_NORMAL = 720;; //minutes => 12 hours
    private final int TASTE_AND_SMELL_IMPROVEMENT = 1440; //minutes => 1 day
    private final int NICOTINE_EXPELLED_FROM_BODY = 4320; //minutes => 3 days
    private final int CIRCULATION_AND_LUNG_FUNCTION = 20160; //minutes => 14 days
    private final int COUGHING_AND_BREATHING = 43200; //minutes => 30 days
    private final int REDUCED_RISK_OF_HEART_DISEASE = 525600; //minutes => 1 year
    private final int STROKE_RISK_REDUCTION = 2628000; //minutes => 5 years
    private final int LUNG_CANCER_RISK_REDUCTION = 5256000; //minutes => 10 years

    @Transactional
    @Override
    public DiaryRecordDTO logDiaryRecord(DiaryRecordRequest request) {
        Member member = memberService.getAuthenticatedMember();
        QuitPlan currentQuitPlan = quitPlanRepository.findTopByMemberIdOrderByCreatedAtDesc(member.getId());
        LocalDate startDate = currentQuitPlan.getStartDate();
        LocalDate currentDate = request.getDate();
        if(currentDate.isBefore(startDate)) {
            throw new RuntimeException("Invalid diary date");
        }

        FormMetric currentFormMetric = currentQuitPlan.getFormMetric();
        //money member spent for cigarettes package
        BigDecimal moneyPerPackage = currentFormMetric.getMoneyPerPackage();
        int cigarettesPerPackage  = currentFormMetric.getCigarettesPerPackage();
        //baseline smoked per day
        int smokeAvgPerDay = currentFormMetric.getSmokeAvgPerDay();
        double _avgDayToSmokeAll = (double) cigarettesPerPackage / smokeAvgPerDay;
        //Number of consumption day
        int avgDayToSmokeAll = (int) Math.ceil(_avgDayToSmokeAll);
        Calendar cal = Calendar.getInstance();
        int numOfDays = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
        //Estimate number of package will be used in a year
        BigDecimal packagesOfYear = BigDecimal.valueOf(numOfDays / avgDayToSmokeAll);
        //Money will be spent in a year
        BigDecimal annualSaved = packagesOfYear.multiply(moneyPerPackage);
        //calculate money saved on plan
        long dayBetween = ChronoUnit.DAYS.between(startDate, currentDate);
        var pricePerCigarettes = moneyPerPackage.divide(BigDecimal.valueOf(cigarettesPerPackage), 1, BigDecimal.ROUND_HALF_UP);
        var moneyForSmokedPerDay = pricePerCigarettes.multiply(BigDecimal.valueOf(smokeAvgPerDay));
        var currentMoneySaved = moneyForSmokedPerDay.multiply(BigDecimal.valueOf(dayBetween));

        double reductionPercentage = ((double) (smokeAvgPerDay - request.getCigarettesSmoked()) /smokeAvgPerDay) * 100.0;

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
        if(!request.getTriggers().isEmpty()){
            diaryRecord.setTriggers(request.getTriggers());
        }
        diaryRecord.setUseNrt(request.getIsUseNrt());
        diaryRecord.setMoneySpentOnNrt(request.getMoneySpentOnNrt());
        diaryRecord.setCravingLevel(request.getCravingLevel());
        diaryRecord.setMoodLevel(request.getMoodLevel());
        diaryRecord.setConfidenceLevel(request.getConfidenceLevel());
        diaryRecord.setAnxietyLevel(request.getAnxietyLevel());
        diaryRecord.setNote(request.getNote());
        diaryRecord.setEstimatedNicotineIntake(estimateNicotineIntake);
        diaryRecord.setConnectIoTDevice(request.getIsConnectIoTDevice());
        diaryRecord.setReductionPercentage(reductionPercentage);
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
                    newMetric.setAnnualSaved(annualSaved);
                    newMetric.setReductionPercentage(reductionPercentage);
                    newMetric.setMoneySaved(currentMoneySaved.subtract(BigDecimal.valueOf(request.getMoneySpentOnNrt())).subtract(pricePerCigarettes.multiply(BigDecimal.valueOf(request.getCigarettesSmoked()))));
                    newMetric.setSmokeFreeDayPercentage(request.getHaveSmoked() == true ? 0.0 : 100.0);
                    newMetric.setAvgCigarettesPerDay(request.getCigarettesSmoked());
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
        Optional<DiaryRecord> previousDayRecord = diaryRecordRepository.findTopByMemberIdOrderByDateDesc(member.getId());
        if(previousDayRecord.isPresent()){
            System.out.println("have previous day record");
            LocalDate date = previousDayRecord.get().getDate();
            LocalDate yesterday = request.getDate().minusDays(1);
            boolean isYesterday = date.isEqual(yesterday);
            System.out.println("isYesterday: " + isYesterday);
            if(isYesterday && request.getHaveSmoked() == false){
                streaksCount = metric.getStreaks() +1;
            }
        }

        int smokeFreeDaysCount = 0;
        int totalCigarettesInRecords = 0;
        List<DiaryRecord> records = diaryRecordRepository.findByMemberId(member.getId());
        for(DiaryRecord record : records){
            if(!record.isHaveSmoked()) {
                //count only days member did not smoke
                smokeFreeDaysCount += 1;
            }
            totalCigarettesInRecords += record.getCigarettesSmoked();
        }
        int count = records.size(); // number of member's diary records
        double newAvgCravingLevel = metric.getAvgCravingLevel() + (request.getCravingLevel() - metric.getAvgCravingLevel()) / (count +1);
        double newAvgMoodLevel = metric.getAvgMood() + (request.getMoodLevel() - metric.getAvgMood()) / (count +1);
        double newAvgConfidenceLevel = metric.getAvgConfidentLevel() + (request.getConfidenceLevel() - metric.getAvgConfidentLevel()) / (count +1);
        double newAvgAnxietyLevel = metric.getAvgAnxiety() + (request.getAnxietyLevel() - metric.getAvgAnxiety()) / (count +1);
        double avgCigarettesPerDay = (double) totalCigarettesInRecords /count;
        if(Double.isNaN(avgCigarettesPerDay) || Double.isInfinite(avgCigarettesPerDay)){
            avgCigarettesPerDay = metric.getAvgCigarettesPerDay();
        }
        double smokeFreeDayPercentage = ((double) smokeFreeDaysCount / dayBetween) * 100.0;
        if(Double.isNaN(smokeFreeDayPercentage) || Double.isInfinite(smokeFreeDayPercentage)){
            smokeFreeDayPercentage = metric.getSmokeFreeDayPercentage();
        }
        metric.setStreaks(request.getHaveSmoked() ? 0 : streaksCount);
        metric.setAvgCravingLevel(newAvgCravingLevel);
        metric.setAvgMood(newAvgMoodLevel);
        metric.setAvgConfidentLevel(newAvgConfidenceLevel);
        metric.setAvgAnxiety(newAvgAnxietyLevel);
        metric.setCurrentAnxietyLevel(request.getAnxietyLevel());
        metric.setCurrentCravingLevel(request.getCravingLevel());
        metric.setCurrentConfidenceLevel(request.getConfidenceLevel());
        metric.setCurrentMoodLevel(request.getMoodLevel());
        metric.setAnnualSaved(annualSaved);
        metric.setMoneySaved(currentMoneySaved.subtract(BigDecimal.valueOf(request.getMoneySpentOnNrt())).subtract(pricePerCigarettes.multiply(BigDecimal.valueOf(request.getCigarettesSmoked()))));
        metric.setAvgCigarettesPerDay(avgCigarettesPerDay);
        metric.setReductionPercentage(reductionPercentage);
        metric.setSmokeFreeDayPercentage(smokeFreeDayPercentage);

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
        diaryRecord = diaryRecordRepository.save(diaryRecord);

        //calculate recovery time
        calculateRecoveryTime(calculateAge(member.getDob()), currentQuitPlan.getFtndScore(), request.getHaveSmoked());

        return diaryRecordMapper.toDiaryRecordDTO(diaryRecord);
    }

    /*
    * Calculate recovery time on WHO data: https://www.who.int/news-room/questions-and-answers/item/tobacco-health-benefits-of-smoking-cessation
    * */
    private void calculateRecoveryTime(int age, int ftndScore, boolean isSmoke){
        Member member = memberService.getAuthenticatedMember();
        HealthRecovery pulseRateRecovery = healthRecoveryRepository.findByNameAndMemberId(HealthRecoveryDataName.PULSE_RATE, member.getId())
                .orElseGet(() -> {
                    HealthRecovery newRecovery = new HealthRecovery();
                    newRecovery.setName(HealthRecoveryDataName.PULSE_RATE);
                    newRecovery.setMember(member);
                    newRecovery.setValue(isSmoke ? BigDecimal.valueOf(80.0) : BigDecimal.valueOf(100.0));
                    var estimateRecoveryTimeInMinutes = calculateTimeToNormal(PULSE_RATE_TO_NORMAL, age, ftndScore);
                    newRecovery.setTimeTriggered(LocalDateTime.now());
                    newRecovery.setRecoveryTime(estimateRecoveryTimeInMinutes);
                    LocalDateTime estimateTargetTime = LocalDateTime.now().plusMinutes((long) estimateRecoveryTimeInMinutes);
                    newRecovery.setTargetTime(isSmoke ? estimateTargetTime : LocalDateTime.now());
                    newRecovery.setDescription("Pulse rate returns to normal");
                    healthRecoveryRepository.save(newRecovery);
                    return newRecovery;
                });
        HealthRecovery oxygenLevelRecovery = healthRecoveryRepository.findByNameAndMemberId(HealthRecoveryDataName.OXYGEN_LEVEL, member.getId())
                .orElseGet(() -> {
                    HealthRecovery newRecovery = new HealthRecovery();
                    newRecovery.setName(HealthRecoveryDataName.OXYGEN_LEVEL);
                    newRecovery.setMember(member);
                    newRecovery.setValue(isSmoke ? BigDecimal.valueOf(90.0) : BigDecimal.valueOf(100.0));
                    var estimateRecoveryTimeInMinutes = calculateTimeToNormal(OXYGEN_LEVEL_TO_NORMAL, age, ftndScore);
                    newRecovery.setTimeTriggered(LocalDateTime.now());
                    newRecovery.setRecoveryTime(estimateRecoveryTimeInMinutes);
                    LocalDateTime estimateTargetTime = LocalDateTime.now().plusMinutes((long) estimateRecoveryTimeInMinutes);
                    newRecovery.setTargetTime(isSmoke ? estimateTargetTime : LocalDateTime.now());
                    newRecovery.setDescription("Oxygen level in blood returns to normal");
                    healthRecoveryRepository.save(newRecovery);
                    return newRecovery;
                });
        HealthRecovery carbonMonoxideRecovery = healthRecoveryRepository.findByNameAndMemberId(HealthRecoveryDataName.CARBON_MONOXIDE_LEVEL, member.getId())
                .orElseGet(() -> {
                    HealthRecovery newRecovery = new HealthRecovery();
                    newRecovery.setName(HealthRecoveryDataName.CARBON_MONOXIDE_LEVEL);
                    newRecovery.setMember(member);
                    newRecovery.setValue(isSmoke ? BigDecimal.valueOf(92.0) : BigDecimal.valueOf(100.0));
                    var estimateRecoveryTimeInMinutes = calculateTimeToNormal(CARBON_MONOXIDE_TO_NORMAL, age, ftndScore);
                    newRecovery.setTimeTriggered(LocalDateTime.now());
                    newRecovery.setRecoveryTime(estimateRecoveryTimeInMinutes);
                    LocalDateTime estimateTargetTime = LocalDateTime.now().plusMinutes((long) estimateRecoveryTimeInMinutes);
                    newRecovery.setTargetTime(isSmoke ? estimateTargetTime : LocalDateTime.now());
                    newRecovery.setDescription("Carbon monoxide level in blood returns to normal");
                    healthRecoveryRepository.save(newRecovery);
                    return newRecovery;
                });
        HealthRecovery tasteAndSmellRecovery = healthRecoveryRepository.findByNameAndMemberId(HealthRecoveryDataName.TASTE_AND_SMELL, member.getId())
                .orElseGet(() -> {
                    HealthRecovery newRecovery = new HealthRecovery();
                    newRecovery.setName(HealthRecoveryDataName.TASTE_AND_SMELL);
                    newRecovery.setMember(member);
                    newRecovery.setValue(isSmoke ? BigDecimal.valueOf(96.0) : BigDecimal.valueOf(100.0));
                    var estimateRecoveryTimeInMinutes = calculateTimeToNormal(TASTE_AND_SMELL_IMPROVEMENT, age, ftndScore);
                    newRecovery.setTimeTriggered(LocalDateTime.now());
                    newRecovery.setRecoveryTime(estimateRecoveryTimeInMinutes);
                    LocalDateTime estimateTargetTime = LocalDateTime.now().plusMinutes((long) estimateRecoveryTimeInMinutes);
                    newRecovery.setTargetTime(isSmoke ? estimateTargetTime : LocalDateTime.now());
                    newRecovery.setDescription("Taste and smell improvement");
                    healthRecoveryRepository.save(newRecovery);
                    return newRecovery;
                });
        HealthRecovery nicotineExpelledFromBodyRecovery = healthRecoveryRepository.findByNameAndMemberId(HealthRecoveryDataName.NICOTINE_EXPELLED_FROM_BODY, member.getId())
                .orElseGet(() -> {
                    HealthRecovery newRecovery = new HealthRecovery();
                    newRecovery.setName(HealthRecoveryDataName.NICOTINE_EXPELLED_FROM_BODY);
                    newRecovery.setMember(member);
                    newRecovery.setValue(isSmoke ? BigDecimal.valueOf(95.0) : BigDecimal.valueOf(100.0));
                    var estimateRecoveryTimeInMinutes = calculateTimeToNormal(NICOTINE_EXPELLED_FROM_BODY, age, ftndScore);
                    newRecovery.setTimeTriggered(LocalDateTime.now());
                    newRecovery.setRecoveryTime(estimateRecoveryTimeInMinutes);
                    LocalDateTime estimateTargetTime = LocalDateTime.now().plusMinutes((long) estimateRecoveryTimeInMinutes);
                    newRecovery.setTargetTime(isSmoke ? estimateTargetTime : LocalDateTime.now());
                    newRecovery.setDescription("Nicotine is expelled from body");
                    healthRecoveryRepository.save(newRecovery);
                    return newRecovery;
                });
        HealthRecovery circulationRecovery = healthRecoveryRepository.findByNameAndMemberId(HealthRecoveryDataName.CIRCULATION, member.getId())
                .orElseGet(() -> {
                    HealthRecovery newRecovery = new HealthRecovery();
                    newRecovery.setName(HealthRecoveryDataName.CIRCULATION);
                    newRecovery.setMember(member);
                    var estimateRecoveryTimeInMinutes = calculateTimeToNormal(CIRCULATION_AND_LUNG_FUNCTION, age, ftndScore);
                    newRecovery.setTimeTriggered(LocalDateTime.now());
                    newRecovery.setRecoveryTime(estimateRecoveryTimeInMinutes);
                    LocalDateTime estimateTargetTime = LocalDateTime.now().plusMinutes((long) estimateRecoveryTimeInMinutes);
                    newRecovery.setTargetTime(isSmoke ? estimateTargetTime : LocalDateTime.now());
                    newRecovery.setDescription("Circulation and lung function improvement");
                    healthRecoveryRepository.save(newRecovery);
                    return newRecovery;
                });
        HealthRecovery coughingAndBreathingRecovery = healthRecoveryRepository.findByNameAndMemberId(HealthRecoveryDataName.BREATHING, member.getId())
                .orElseGet(() -> {
                    HealthRecovery newRecovery = new HealthRecovery();
                    newRecovery.setName(HealthRecoveryDataName.BREATHING);
                    newRecovery.setMember(member);
                    var estimateRecoveryTimeInMinutes = calculateTimeToNormal(COUGHING_AND_BREATHING, age, ftndScore);
                    newRecovery.setTimeTriggered(LocalDateTime.now());
                    newRecovery.setRecoveryTime(estimateRecoveryTimeInMinutes);
                    LocalDateTime estimateTargetTime = LocalDateTime.now().plusMinutes((long) estimateRecoveryTimeInMinutes);
                    newRecovery.setTargetTime(isSmoke ? estimateTargetTime : LocalDateTime.now());
                    newRecovery.setDescription("Coughing and breathing improvement");
                    healthRecoveryRepository.save(newRecovery);
                    return newRecovery;
                });
        HealthRecovery reducedRiskOfHeartDiseaseRecovery = healthRecoveryRepository.findByNameAndMemberId(HealthRecoveryDataName.REDUCED_RISK_OF_HEART_DISEASE, member.getId())
                .orElseGet(() -> {
                    HealthRecovery newRecovery = new HealthRecovery();
                    newRecovery.setName(HealthRecoveryDataName.REDUCED_RISK_OF_HEART_DISEASE);
                    newRecovery.setMember(member);
                    var estimateRecoveryTimeInMinutes = calculateTimeToNormal(REDUCED_RISK_OF_HEART_DISEASE, age, ftndScore);
                    newRecovery.setTimeTriggered(LocalDateTime.now());
                    newRecovery.setRecoveryTime(estimateRecoveryTimeInMinutes);
                    LocalDateTime estimateTargetTime = LocalDateTime.now().plusMinutes((long) estimateRecoveryTimeInMinutes);
                    newRecovery.setTargetTime(isSmoke ? estimateTargetTime : LocalDateTime.now());
                    newRecovery.setDescription("Reduced risk of heart disease");
                    healthRecoveryRepository.save(newRecovery);
                    return newRecovery;
                });
        HealthRecovery reducedRiskOfHeartAttackRecovery = healthRecoveryRepository.findByNameAndMemberId(HealthRecoveryDataName.DECREASED_RISK_OF_HEART_ATTACK, member.getId())
                .orElseGet(() -> {
                    HealthRecovery newRecovery = new HealthRecovery();
                    newRecovery.setName(HealthRecoveryDataName.DECREASED_RISK_OF_HEART_ATTACK);
                    newRecovery.setMember(member);
                    var estimateRecoveryTimeInMinutes = calculateTimeToNormal(STROKE_RISK_REDUCTION, age, ftndScore);
                    newRecovery.setTimeTriggered(LocalDateTime.now());
                    newRecovery.setRecoveryTime(estimateRecoveryTimeInMinutes);
                    LocalDateTime estimateTargetTime = LocalDateTime.now().plusMinutes((long) estimateRecoveryTimeInMinutes);
                    newRecovery.setTargetTime(isSmoke ? estimateTargetTime : LocalDateTime.now());
                    newRecovery.setDescription("Stroke risk and Heart attack reduction");
                    healthRecoveryRepository.save(newRecovery);
                    return newRecovery;
                });
        HealthRecovery immunityAndLungFunctionRecovery = healthRecoveryRepository.findByNameAndMemberId(HealthRecoveryDataName.IMMUNITY_AND_LUNG_FUNCTION, member.getId())
                .orElseGet(() -> {
                    HealthRecovery newRecovery = new HealthRecovery();
                    newRecovery.setName(HealthRecoveryDataName.IMMUNITY_AND_LUNG_FUNCTION);
                    newRecovery.setMember(member);
                    var estimateRecoveryTimeInMinutes = calculateTimeToNormal(LUNG_CANCER_RISK_REDUCTION, age, ftndScore);
                    newRecovery.setTimeTriggered(LocalDateTime.now());
                    newRecovery.setRecoveryTime(estimateRecoveryTimeInMinutes);
                    LocalDateTime estimateTargetTime = LocalDateTime.now().plusMinutes((long) estimateRecoveryTimeInMinutes);
                    newRecovery.setTargetTime(isSmoke ? estimateTargetTime : LocalDateTime.now());
                    newRecovery.setDescription("Your risk of lung cancer falls to about half that of a smoker and your risk of cancer of the mouth, throat, esophagus, bladder, cervix, and pancreas decreases.");
                    healthRecoveryRepository.save(newRecovery);
                    return newRecovery;
                });

        if(isSmoke){
            updateRecoveryTimeIfSmoked(pulseRateRecovery, PULSE_RATE_TO_NORMAL, age, ftndScore);
            updateRecoveryTimeIfSmoked(oxygenLevelRecovery, OXYGEN_LEVEL_TO_NORMAL, age, ftndScore);
            updateRecoveryTimeIfSmoked(carbonMonoxideRecovery, CARBON_MONOXIDE_TO_NORMAL, age, ftndScore);
            updateRecoveryTimeIfSmoked(tasteAndSmellRecovery, TASTE_AND_SMELL_IMPROVEMENT, age, ftndScore);
            updateRecoveryTimeIfSmoked(nicotineExpelledFromBodyRecovery, NICOTINE_EXPELLED_FROM_BODY, age, ftndScore);
            updateRecoveryTimeIfSmoked(circulationRecovery, CIRCULATION_AND_LUNG_FUNCTION, age, ftndScore);
            updateRecoveryTimeIfSmoked(coughingAndBreathingRecovery, COUGHING_AND_BREATHING, age, ftndScore);
            updateRecoveryTimeIfSmoked(reducedRiskOfHeartDiseaseRecovery, REDUCED_RISK_OF_HEART_DISEASE, age, ftndScore);
            updateRecoveryTimeIfSmoked(reducedRiskOfHeartAttackRecovery, STROKE_RISK_REDUCTION, age, ftndScore);
            updateRecoveryTimeIfSmoked(immunityAndLungFunctionRecovery, LUNG_CANCER_RISK_REDUCTION, age, ftndScore);
        }

    }

    private void updateRecoveryTimeIfSmoked(HealthRecovery recovery,
                                            int baseTimeInMinutes,
                                            int age,
                                            int ftndScore
    ){
        var estimateRecoveryTimeInMinutes = calculateTimeToNormal(baseTimeInMinutes, age, ftndScore);
        recovery.setRecoveryTime(estimateRecoveryTimeInMinutes);
        LocalDateTime newTargetTime = LocalDateTime.now().plusMinutes((long) estimateRecoveryTimeInMinutes);
        recovery.setTargetTime(newTargetTime);
        healthRecoveryRepository.save(recovery);
    }

    private double calculateTimeToNormal(int baseTimeInMinutes, int age, int ftndScore){
        return baseTimeInMinutes * (1 + 0.02 * Math.max(0, age - 30)) * (1 + 0.08 * Math.max(0, ftndScore - 3));
    }

    private int calculateAge(LocalDate dob){
        return Period.between(dob, LocalDate.now()).getYears();
    }

    @Override
    public List<DiaryRecordDTO> getDiaryRecordsForMember() {
        Member member = memberService.getAuthenticatedMember();
        List<DiaryRecord> records = diaryRecordRepository.findByMemberId(member.getId());
        return records.stream().map(diaryRecordMapper::toListDiaryRecordDTO).toList();
    }

    @Override
    public DiaryRecordDTO getDiaryRecordById(Integer id) {
        DiaryRecord record = diaryRecordRepository.findById(id).orElseThrow(() -> new RuntimeException("Diary record not found"));
        return diaryRecordMapper.toDiaryRecordDTO(record);
    }

    @Override
    public Map<String, Object> getDiaryRecordsCharts() {
        Member member = memberService.getAuthenticatedMember();
        List<DiaryRecord> records = diaryRecordRepository.findByMemberId(member.getId());
        Map<String, Object> chartsData = new HashMap<>();
        chartsData.put("confidenceLevel", records.stream().map(record -> Map.of(
                "date", record.getDate(),
                "confidenceLevel", record.getConfidenceLevel()
        )).toList());
        chartsData.put("moodLevel", records.stream().map(record -> Map.of(
                "date", record.getDate(),
                "moodLevel", record.getMoodLevel()
        )).toList());
        chartsData.put("anxietyLevel", records.stream().map(record -> Map.of(
                "date", record.getDate(),
                "anxietyLevel", record.getAnxietyLevel()
        )).toList());
        chartsData.put("cravingLevel", records.stream().map(record -> Map.of(
                "date", record.getDate(),
                "cravingLevel", record.getCravingLevel()
        )).toList());
        return chartsData;
    }
}

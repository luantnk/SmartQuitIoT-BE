package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.AiPredictionRequest;
import com.smartquit.smartquitiot.dto.request.CreateNewQuitPlanRequest;
import com.smartquit.smartquitiot.dto.request.CreateQuitPlanInFirstLoginRequest;
import com.smartquit.smartquitiot.dto.request.KeepPhaseOfQuitPlanRequest;
import com.smartquit.smartquitiot.dto.response.*;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.enums.NotificationType;
import com.smartquit.smartquitiot.enums.PhaseStatus;
import com.smartquit.smartquitiot.enums.QuitPlanStatus;
import com.smartquit.smartquitiot.mapper.QuitPlanMapper;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "quit_plans")
public class QuitPlanServiceImpl implements QuitPlanService {
    private final QuitPlanRepository quitPlanRepository;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final PhaseDetailService phaseDetailService;
    private final PhaseDetailMissionService  phaseDetailMissionService;
    private final PhaseService phaseService;
    private final QuitPlanMapper quitPlanMapper;
    private final MissionRepository missionRepository;
    private final PhaseRepository phaseRepository;
    private final NotificationService  notificationService;
    private final String PHASE_PRE = "Preparation";
    private final int MAX_MISSIONS = 4;
    private final DiaryRecordRepository diaryRecordRepository;
    private final RestTemplate restTemplate;
    private final String AI_SERVICE_URL = "http://localhost:8000/predict-quit-status";
    @Transactional
    @Override
    public PhaseBatchMissionsResponse createQuitPlanInFirstLogin(CreateQuitPlanInFirstLoginRequest req) {
        Account account = accountService.getAuthenticatedAccount();
        List<Mission> allMissions = missionRepository.findAll();
        if(allMissions.isEmpty()) {
            throw new RuntimeException("Mission library is empty, Need insert missions library!");
        }
        int ftndScore = calculateFTNDScore(req);
        if (account.isFirstLogin()) {
            //get response from ai
            PhaseResponse phaseResponse = phaseService.generatePhasesInFirstLogin(req, ftndScore, account);

            // save quit plan
            QuitPlan quitPlan = new QuitPlan();
            quitPlan.setName(req.getQuitPlanName());
            quitPlan.setFtndScore(ftndScore);
            quitPlan.setMember(account.getMember());
            quitPlan.setStartDate(req.getStartDate());
            LocalDate currentDate = LocalDate.now();
            
            if(req.getStartDate().equals(currentDate)){
                quitPlan.setStatus(QuitPlanStatus.IN_PROGRESS);
            }else{
                quitPlan.setStatus(QuitPlanStatus.CREATED);
            }

            quitPlan.setUseNRT(req.isUseNRT());
            //from metric
            FormMetric formMetric = new FormMetric();
            formMetric.setSmokeAvgPerDay(req.getSmokeAvgPerDay());
            formMetric.setNumberOfYearsOfSmoking(req.getNumberOfYearsOfSmoking());
            formMetric.setCigarettesPerPackage(req.getCigarettesPerPackage());
            formMetric.setMinutesAfterWakingToSmoke(req.getMinutesAfterWakingToSmoke());
            formMetric.setSmokingInForbiddenPlaces(req.isSmokingInForbiddenPlaces());
            formMetric.setCigaretteHateToGiveUp(req.isCigaretteHateToGiveUp());
            formMetric.setMorningSmokingFrequency(req.isMorningSmokingFrequency());
            formMetric.setSmokeWhenSick(req.isSmokeWhenSick());
            formMetric.setMoneyPerPackage(req.getMoneyPerPackage());
            formMetric.setEstimatedMoneySavedOnPlan(calculateMoneySavedOnPlan(req.getStartDate(),
                    phaseResponse.getEndDateOfQuitPlan(), req.getCigarettesPerPackage(),
                    req.getMoneyPerPackage(), req.getSmokeAvgPerDay()));
            formMetric.setAmountOfNicotinePerCigarettes(req.getAmountOfNicotinePerCigarettes());
            formMetric.setEstimatedNicotineIntakePerDay(calculateNicotineIntakePerDay(req.getAmountOfNicotinePerCigarettes(), req.getSmokeAvgPerDay()));
            formMetric.setInterests(req.getInterests());
            //  formMetric.setTriggered(req.getTriggered());
            quitPlan.setFormMetric(formMetric);
            quitPlan.setEndDate(phaseResponse.getEndDateOfQuitPlan());
            //save quit plan and form metric
            quitPlanRepository.save(quitPlan);
            // set account is first login ve false
            account.setFirstLogin(false);
            accountRepository.save(account);
            //save phase and system phase condition
            phaseService.savePhasesAndSystemPhaseCondition(phaseResponse, quitPlan);
            //tao phase detail
            List<PhaseDetail> preparedDetails = phaseDetailService.generateInitialPhaseDetails(quitPlan,PHASE_PRE);
            return phaseDetailMissionService.generatePhaseDetailMissionsForPhase
                    (preparedDetails,quitPlan, MAX_MISSIONS, PHASE_PRE, MissionPhase.PREPARATION);

       }
        else {
            throw new RuntimeException("isFirstLogin is FALSE in createQuitPlanInFirstLogin function ");
        }
    }

    @Override
    public QuitPlanResponse getCurrentQuitPlan() {
        Account account = accountService.getAuthenticatedAccount();
        QuitPlan plan = quitPlanRepository.findByMember_IdAndIsActiveTrue(account.getMember().getId());
        if(plan == null) {
            throw new RuntimeException("No Quit Plan found when using!");
        }
        return quitPlanMapper.toResponse(plan);
    }

    @Override
    public TimeResponse getCurrentTimeOfQuitPlan() {
        Account account = accountService.getAuthenticatedAccount();
        QuitPlan plan = quitPlanRepository.findByMember_IdAndIsActiveTrue(account.getMember().getId());
        if (plan == null) {
            throw new RuntimeException("No Quit Plan found when using!");
        }
        TimeResponse timeResponse = new TimeResponse();

        if(plan.getStartDate().isAfter(LocalDate.now())) {
            timeResponse.setStartTime(plan.getStartDate().atStartOfDay());
        }else{
            timeResponse.setStartTime(plan.getCreatedAt());
        }
        return timeResponse;
    }

    @Override
    public QuitPlanResponse getMemberQuitPlan(int memberId) {
        QuitPlan plan = quitPlanRepository.findByMember_IdAndIsActiveTrue(memberId);
        return quitPlanMapper.toResponse(plan);
    }

    public QuitPlanResponse keepPhaseOfQuitPlan(KeepPhaseOfQuitPlanRequest request) {
        Phase phase = phaseRepository.findByIdAndQuitPlan_Id(request.getPhaseId(),request.getQuitPlanId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Phase not found for planId=" + request.getQuitPlanId() + ", phaseId=" + request.getPhaseId()));
        Account me = accountService.getAuthenticatedAccount();
        int ownerId = phase.getQuitPlan().getMember().getId();
        log.info("ownerId: {} ", ownerId);
        if (me.getMember().getId() != ownerId) {
            throw new RuntimeException("You do not own this quit plan");
        }
        if(phase.getStatus() == PhaseStatus.FAILED){
            phase.setKeepPhase(true);
        }else{
            throw new RuntimeException("Phase is not in Failed status so cant not keep phase of quit plan");
        }

        return quitPlanMapper.toResponse(phaseRepository.save(phase).getQuitPlan());
    }

    @Override
    @Transactional
    public PhaseBatchMissionsResponse createNewQuitPlan(CreateNewQuitPlanRequest req) {
        Account account = accountService.getAuthenticatedAccount();
        List<Mission> allMissions = missionRepository.findAll();
        if(allMissions.isEmpty()) {
            throw new RuntimeException("Mission library is empty, Need insert missions library!");
        }

        QuitPlan oldQuitPlan = quitPlanRepository.findByMember_IdAndIsActiveTrue(account.getMember().getId());
        oldQuitPlan.setActive(false);
        if(oldQuitPlan.getStatus() == QuitPlanStatus.IN_PROGRESS || oldQuitPlan.getStatus() == QuitPlanStatus.CREATED) {
            oldQuitPlan.setStatus(QuitPlanStatus.CANCELED);
        }
        quitPlanRepository.save(oldQuitPlan);
        FormMetric newFormMetric = cloneFormMetric(oldQuitPlan.getFormMetric());
        if(newFormMetric == null) {
            throw new RuntimeException("newFormMetric get from clone old metric is null!");
        }
            //get response from ai
            PhaseResponse phaseResponse = phaseService.generatePhases
                    (newFormMetric.getSmokeAvgPerDay(),newFormMetric.getNumberOfYearsOfSmoking(),
                            req.getStartDate(), oldQuitPlan.getFtndScore(), account);

            // save quit plan
            QuitPlan newQuitPlan = new QuitPlan();
            newQuitPlan.setFormMetric(newFormMetric);
            newQuitPlan.setName(req.getQuitPlanName());
            newQuitPlan.setFtndScore(oldQuitPlan.getFtndScore());
            newQuitPlan.setMember(account.getMember());
            newQuitPlan.setStartDate(req.getStartDate());
            newQuitPlan.setUseNRT(req.isUseNRT());
            LocalDate currentDate = LocalDate.now();

            if(req.getStartDate().equals(currentDate)){
                newQuitPlan.setStatus(QuitPlanStatus.IN_PROGRESS);
            }else{
                newQuitPlan.setStatus(QuitPlanStatus.CREATED);
            }

            newQuitPlan.setEndDate(phaseResponse.getEndDateOfQuitPlan());
            //save quit plan and form metric
            quitPlanRepository.save(newQuitPlan);

            //save phase and system phase condition
            phaseService.savePhasesAndSystemPhaseCondition(phaseResponse, newQuitPlan);
            //tao phase detail
            List<PhaseDetail> preparedDetails = phaseDetailService.generateInitialPhaseDetails(newQuitPlan,"Preparation");

            PhaseBatchMissionsResponse phaseBatchMissionsResponse = phaseDetailMissionService.generatePhaseDetailMissionsForPhase
                    (preparedDetails,newQuitPlan, MAX_MISSIONS, PHASE_PRE, MissionPhase.PREPARATION);
            if(phaseBatchMissionsResponse != null) {
                notificationService.saveAndPublish(account.getMember().getAccount(), NotificationType.QUIT_PLAN,
                        "Created New Quit Plan!",
                        "New Quit Plan already created for you! Can do it better than you think <3",
                        null, null,null
                        );
                return phaseBatchMissionsResponse;
            }else {
                throw new RuntimeException("phaseBatchMissionsResponse is null!");
            }


    }

    @Override
    public List<QuitPlanResponse> getHistory() {
        Account account = accountService.getAuthenticatedAccount();
        List<QuitPlan> quitPlans = quitPlanRepository.findAllByMember_IdOrderByCreatedAtDesc(account.getMember().getId());
        return quitPlanMapper.toViewAll(quitPlans);
    }

    @Override
    public QuitPlanResponse getSpecific(int id) {
        Account account = accountService.getAuthenticatedAccount();
        QuitPlan plan = quitPlanRepository.findByMember_IdAndId(account.getMember().getId(), id)
                .orElseThrow(() -> new EntityNotFoundException("QuitPlan not found or not belong to this member!"));
        return quitPlanMapper.toResponse(plan);
    }


    private BigDecimal calculateNicotineIntakePerDay(BigDecimal amountOfNicotinePerCigarettes, int smokeAvgPerDay) {
        return amountOfNicotinePerCigarettes.multiply(BigDecimal.valueOf(smokeAvgPerDay));
    }

    private BigDecimal calculateMoneySavedOnPlan(LocalDate startDate, LocalDate endDate, int cigarettesPerPackage, BigDecimal moneyPerPackage, int smokeAvgPerDay) {
        // day between start date and endDate, Note: end date is last day of maintenance phases.
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        // avg money per cigarette
        BigDecimal pricePerCigarette = moneyPerPackage
                .divide(BigDecimal.valueOf(cigarettesPerPackage), 2, RoundingMode.HALF_UP);
        // total
        return pricePerCigarette
                .multiply(BigDecimal.valueOf(smokeAvgPerDay))
                .multiply(BigDecimal.valueOf(days));

    }

    private int calculateFTNDScore(CreateQuitPlanInFirstLoginRequest req) {
        //https://www.aarc.org/wp-content/uploads/2014/08/Fagerstrom_test.pdf
        int score = 0;
        // 1) How soon after waking do you smoke your first cigarette?
        // ≤5 phút: 3 điểm; 6–30: 2; 31–60: 1; >60: 0
        int m = req.getMinutesAfterWakingToSmoke();
        if (m <= 5) score += 3;
        else if (m <= 30) score += 2;
        else if (m <= 60) score += 1;
        // 2) Difficult to refrain in forbidden places? Church, library,...Yes = 1, No = 0
        if (req.isSmokingInForbiddenPlaces()) score += 1;
        // 3) Which cigarette would you hate to give up? First in the morning = 1, Any other = 0
        // Map: cigaretteHateToGiveUp == true  -> "First in the morning"
        if (req.isCigaretteHateToGiveUp()) score += 1;
        // 4) How many cigarettes a day?
        // 10 or less: 0; 11–20: 1; 21–30: 2; 31 or more: 3
        int cpd = req.getSmokeAvgPerDay();
        if (cpd >= 31) score += 3;
        else if (cpd >= 21) score += 2;
        else if (cpd >= 11) score += 1;
        // 5) Smoke more frequently in the morning? Yes = 1
        if (req.isMorningSmokingFrequency()) score += 1;
        // 6) Smoke even if sick in bed most of the day? Yes = 1
        if (req.isSmokeWhenSick()) score += 1;
        return score;
    }


    private FormMetric cloneFormMetric(FormMetric oldSrc) {
        if (oldSrc == null) return null;
        FormMetric fm = new FormMetric();
        fm.setSmokeAvgPerDay(oldSrc.getSmokeAvgPerDay());
        fm.setNumberOfYearsOfSmoking(oldSrc.getNumberOfYearsOfSmoking());
        fm.setCigarettesPerPackage(oldSrc.getCigarettesPerPackage());
        fm.setMinutesAfterWakingToSmoke(oldSrc.getMinutesAfterWakingToSmoke());
        fm.setSmokingInForbiddenPlaces(oldSrc.isSmokingInForbiddenPlaces());
        fm.setCigaretteHateToGiveUp(oldSrc.isCigaretteHateToGiveUp());
        fm.setMorningSmokingFrequency(oldSrc.isMorningSmokingFrequency());
        fm.setSmokeWhenSick(oldSrc.isSmokeWhenSick());
        fm.setMoneyPerPackage(oldSrc.getMoneyPerPackage());
        fm.setEstimatedMoneySavedOnPlan(oldSrc.getEstimatedMoneySavedOnPlan());
        fm.setAmountOfNicotinePerCigarettes(oldSrc.getAmountOfNicotinePerCigarettes());
        fm.setEstimatedNicotineIntakePerDay(oldSrc.getEstimatedNicotineIntakePerDay());
        fm.setInterests(oldSrc.getInterests());
        fm.setTriggered(oldSrc.getTriggered());

        return fm;
    }

    @Override
    public AiPredictionResponse getPredictionForCurrentPlan() {
        Account account = accountService.getAuthenticatedAccount();
        Member member = account.getMember();
        QuitPlan plan = quitPlanRepository.findByMember_IdAndIsActiveTrue(member.getId());

        if (plan == null) {
            throw new RuntimeException("No active Quit Plan found to predict!");
        }

        Optional<DiaryRecord> latestDiaryOpt = diaryRecordRepository.findFirstByMember_IdOrderByDateDesc(member.getId());

        float anxiety = 0f;
        float craving = 0f;
        float mood = 0f;
        float heartRate = 0f;
        float sleep = 0f;

        if (latestDiaryOpt.isPresent()) {
            DiaryRecord dr = latestDiaryOpt.get();
            anxiety = (float) dr.getAnxietyLevel();
            craving = (float) dr.getCravingLevel();
            mood = (float) dr.getMoodLevel();
            heartRate = (float) dr.getHeartRate();
            sleep = (float) dr.getSleepDuration();
        }

        FormMetric metric = plan.getFormMetric();

        int age = 0;
        if (member.getDob() != null) {
            age = LocalDate.now().getYear() - member.getDob().getYear();
        }

        float genderCode = (member.getGender() != null && member.getGender().name().equalsIgnoreCase("MALE")) ? 1.0f : 0.0f;

        float progress = 0;
        if (!plan.getPhases().isEmpty()) {
            if(plan.getPhases().get(0).getProgress() != null) {
                progress = plan.getPhases().get(0).getProgress().floatValue();
            }
        }

        List<Float> features = Arrays.asList(
                (float) plan.getFtndScore(),
                (float) metric.getSmokeAvgPerDay(),
                (float) metric.getMinutesAfterWakingToSmoke(),
                (float) age,
                genderCode,
                anxiety,    // 0 nếu không có diary
                craving,    // 0 nếu không có diary
                mood,       // 0 nếu không có diary
                heartRate,  // 0 nếu không có diary
                sleep,      // 0 nếu không có diary
                progress
        );

        try {
            AiPredictionRequest request = new AiPredictionRequest(features);
            log.info("Calling AI Service with REAL features (0 if missing): {}", features);
            return restTemplate.postForObject(AI_SERVICE_URL, request, AiPredictionResponse.class);
        } catch (Exception e) {
            log.error("Error calling AI Service: ", e);
            AiPredictionResponse fallback = new AiPredictionResponse();
            fallback.setSuccessProbability(0);
            fallback.setRelapseRisk(0);
            fallback.setRecommendation("AI Service unavailable");
            return fallback;
        }
    }

}
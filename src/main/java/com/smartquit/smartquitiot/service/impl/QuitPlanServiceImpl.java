package com.smartquit.smartquitiot.service.impl;
import com.smartquit.smartquitiot.dto.request.CreateQuitPlanInFirstLoginRequest;
import com.smartquit.smartquitiot.dto.response.*;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.enums.QuitPlanStatus;
import com.smartquit.smartquitiot.mapper.QuitPlanMapper;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class QuitPlanServiceImpl implements QuitPlanService {
    private final QuitPlanRepository quitPlanRepository;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final PhaseDetailService phaseDetailService;
    private final PhaseDetailMissionService  phaseDetailMissionService;
    private final PhaseService phaseService;
    private final QuitPlanMapper quitPlanMapper;
    private final MissionRepository missionRepository;

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
            quitPlan.setStatus(QuitPlanStatus.CREATED);
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
            List<PhaseDetail> preparedDetails = phaseDetailService.generateInitialPhaseDetails(quitPlan,"Preparation");

            return phaseDetailMissionService.generatePhaseDetailMissionsForPhase
                    (preparedDetails,quitPlan, 4, "Preparation", MissionPhase.PREPARATION);

        } else {
            throw new RuntimeException("isFirstLogin is FALSE in createQuitPlanInFirstLogin function ");
        }
    }

    @Override
    public QuitPlanResponse getCurrentQuitPlan() {
        QuitPlan plan = quitPlanRepository.findTopByOrderByCreatedAtDesc();
        if (plan == null) {
            throw new RuntimeException("No QuitPlan found");
        }
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



}
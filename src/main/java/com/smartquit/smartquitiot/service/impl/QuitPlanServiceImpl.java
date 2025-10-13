package com.smartquit.smartquitiot.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartquit.smartquitiot.dto.request.CreateQuitPlanInFirstLoginRequest;
import com.smartquit.smartquitiot.dto.response.PhaseDTO;
import com.smartquit.smartquitiot.dto.response.PhaseResponse;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.PhaseStatus;
import com.smartquit.smartquitiot.enums.QuitPlanStatus;
import com.smartquit.smartquitiot.repository.AccountRepository;
import com.smartquit.smartquitiot.repository.PhaseRepository;
import com.smartquit.smartquitiot.repository.QuitPlanRepository;
import com.smartquit.smartquitiot.repository.SystemPhaseConditionRepository;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.PhaseDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuitPlanServiceImpl {
    private final QuitPlanRepository quitPlanRepository;
    private final ChatClient chatClient;
    private final SystemPhaseConditionRepository systemPhaseConditionRepository;
    private final PhaseRepository phaseRepository;
    private final ObjectMapper objectMapper;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final PhaseDetailService phaseDetailService;

    @Transactional
    public void createQuitPlanInFirstLogin(CreateQuitPlanInFirstLoginRequest req) {
        Account account = accountService.getAuthenticatedAccount();
        int ftndScore = calculateFTNDScore(req);
       if(account.isFirstLogin()){
           //get response from ai
           PhaseResponse phaseResponse = generatePhases(req, ftndScore, account);

           // save quit plan
           QuitPlan quitPlan = new QuitPlan();
           quitPlan.setName(req.getQuitPlanName());
           quitPlan.setFtndScore(ftndScore);
           quitPlan.setMemberId(account.getMember().getId());
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
           formMetric.setEstimatedNicotineIntakePerDay(calculateNicotineIntakePerDay(req.getAmountOfNicotinePerCigarettes(),req.getSmokeAvgPerDay()));
           formMetric.setInterests(req.getInterests());
           formMetric.setTriggered(req.getTriggered());
           quitPlan.setFormMetric(formMetric);
           //save quit plan and form metric
           quitPlanRepository.save(quitPlan);
           // set account is first login ve false
           account.setFirstLogin(false);
           accountRepository.save(account);
           //save phase and system phase condition
           savePhasesAndSystemPhaseCondition(phaseResponse,quitPlan);

           //tao phase detail, loc mission va gen mission
           phaseDetailService.generateInitialPhaseDetails(quitPlan);


       }else {
           throw new RuntimeException("isFirstLogin is FALSE in createQuitPlanInFirstLogin function ");
       }
    }


    private PhaseResponse generatePhases(CreateQuitPlanInFirstLoginRequest req, int ftndScore, Account account) {
        //rules
        String system = """
                    You are a smoking cessation assistant.
                    Generate a personalized quit plan with exactly 5 sequential phases:
                    1. Preparation
                    2. Onset
                    3. Peak Craving
                    4. Subsiding
                    5. Maintenance
                
                    Rules for phases:
                
                    - Preparation:
                      * Start at quit plan startDate.
                      * 2–3 days if FTND >= 7 or smokeAvgPerDay > 20.
                      * 1–2 days if FTND <= 3 and smokeAvgPerDay < 10.
                      * Otherwise default 2 days.
                
                    - Onset:
                      * Follows Preparation immediately.
                      * 5–7 days if FTND >= 7 or smokeAvgPerDay > 15.
                      * 3–4 days if FTND <= 3.
                      * Females may have slightly longer Onset (+1–2 days) due to hormonal fluctuations.
                
                    - Peak Craving:
                      * Hardest period, follows Onset.
                      * 7-10 days if FTND >= 7 or yearsSmoking > 10.
                      * 4–6 days if FTND <= 3 and smokeAvgPerDay < 10.
                      * Females may extend 2 days longer due to stronger craving response.
                
                    - Subsiding:
                      * After Peak Craving.
                      * 14–18 days if FTND >= 7.
                      * 10–13 days if FTND <= 3.
                      * If age >= 50, add +3 days (slower physiological recovery).
                
                    - Maintenance:
                      * Last and longest phase.
                      * At least 60–90 days if yearsSmoking > 10 or age >= 50.
                      * At least 45–60 days if 5 < yearsSmoking <= 10.
                      * At least 30 days if yearsSmoking <= 5 and FTND <= 3.
                      * Should be longer for females (add +7 days) to account for higher relapse risk.
                
                    General rules:
                    - Phases must be sequential, no overlaps, and all dates continuous.
                    - Always map total duration realistically based on dependency severity and user profile.
                    - Reason: provide reason why choose with data
                """;

        String userInfo = """
                    User profile:
                    - Age: %s
                    - Gender: %s
                    - smokeAvgPerDay: %d
                    - yearsSmoking: %d
                    - FTND: %d
                    - StartDate: %s
                """.formatted(
                calculateAge(account.getMember().getDob()),
                account.getMember().getGender(),
                req.getSmokeAvgPerDay(),
                req.getNumberOfYearsOfSmoking(),
                ftndScore,
                req.getStartDate());
        // response from ai
        PhaseResponse phaseResponse = chatClient.prompt()
                .system(system)
                .user(userInfo)
                .call()
                .entity(PhaseResponse.class);

        if (phaseResponse == null || phaseResponse.getPhases() == null || phaseResponse.getPhases().isEmpty()) {
            throw new IllegalStateException("AI did not return any phases");
        }

        return phaseResponse;
    }

    private void savePhasesAndSystemPhaseCondition(PhaseResponse phaseResponse, QuitPlan quitPlan) {
        List<SystemPhaseCondition> allConditions = systemPhaseConditionRepository.findAll();
        if (allConditions.isEmpty()) {
            throw new IllegalStateException("No conditions found");
        }

        for (int i = 0; i < phaseResponse.getPhases().size(); i++) {
            PhaseDTO dto = phaseResponse.getPhases().get(i);

            Phase phase = new Phase();
            phase.setName(dto.getName());
            phase.setStartDate(dto.getStartDate());
            phase.setEndDate(dto.getEndDate());
            phase.setDurationDays(dto.getDurationDay());
            phase.setReason(dto.getReason());
            phase.setQuitPlan(quitPlan);
            phase.setStatus(PhaseStatus.CREATED);
            phase.setSystemPhaseCondition(allConditions.get(i)); // set theo thu tu trong condition, dung de xem nguon goc condition
            phase.setCondition(allConditions.get(i).getCondition()); // day moi la condition dung de kiem tra

            phaseRepository.save(phase);
        }
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

    private int calculateAge(LocalDate dob) {
        return Period.between(dob, LocalDate.now()).getYears();
    }

}

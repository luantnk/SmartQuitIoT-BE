package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.UpdateFormMetricRequest;
import com.smartquit.smartquitiot.dto.response.FormMetricDTO;
import com.smartquit.smartquitiot.dto.response.GetFormMetricResponse;
import com.smartquit.smartquitiot.dto.response.UpdateFormMetricResponse;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.FormMetric;
import com.smartquit.smartquitiot.entity.InterestCategory;
import com.smartquit.smartquitiot.entity.QuitPlan;
import com.smartquit.smartquitiot.mapper.FormMetricMapper;
import com.smartquit.smartquitiot.repository.FormMetricRepository;
import com.smartquit.smartquitiot.repository.InterestCategoryRepository;
import com.smartquit.smartquitiot.repository.QuitPlanRepository;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.FormMetricService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormMetricServiceImpl implements FormMetricService {
    private final FormMetricRepository formMetricRepository;
    private final AccountService accountService;
    private final QuitPlanRepository quitPlanRepository;
    private final FormMetricMapper formMetricMapper;
    private static final Set<String> ALLOWED_TRIGGERS = Set.of(
            "Morning", "After Meal", "Gaming", "Party", "Coffee",
            "Stress", "Boredom", "Driving", "Sadness", "Work"
    );
    private final InterestCategoryRepository interestCategoryRepository;

    @Override
    public GetFormMetricResponse getMyFormMetric() {
        Account account = accountService.getAuthenticatedAccount();
        QuitPlan currentQuitPlan = quitPlanRepository.findByMember_IdAndIsActiveTrue(account.getMember().getId());
        if(currentQuitPlan==null){
            throw new RuntimeException("Current Plan is null");
        }
        if(currentQuitPlan.getFormMetric()==null){
            throw new RuntimeException("Current FormMetric is null");
        }
        return formMetricMapper.toDTOByGetFormMetric(currentQuitPlan.getFormMetric(),currentQuitPlan.getFtndScore());
    }
    @Override
    @Transactional
    public UpdateFormMetricResponse updateMyFormMetric(UpdateFormMetricRequest req) {
        Account account = accountService.getAuthenticatedAccount();

        QuitPlan plan = quitPlanRepository.findByMember_IdAndIsActiveTrue(account.getMember().getId());
        if (plan == null) {
            throw new IllegalStateException("Active Quit Plan not found for member " + account.getMember().getId());
        }

        FormMetric fm = plan.getFormMetric();
        if (fm == null) {
            throw new IllegalStateException("FormMetric not found in current Quit Plan " + plan.getId());
        }

        //OLD values
        int oldSmokeAvgPerDay = fm.getSmokeAvgPerDay();
        int oldMinutesAfterWakingToSmoke = fm.getMinutesAfterWakingToSmoke();
        int oldNumberOfYearsOfSmoking = fm.getNumberOfYearsOfSmoking();
        boolean oldSmokingInForbiddenPlaces = fm.isSmokingInForbiddenPlaces();
        boolean oldCigaretteHateToGiveUp = fm.isCigaretteHateToGiveUp();
        boolean oldMorningSmokingFrequency = fm.isMorningSmokingFrequency();
        boolean oldSmokeWhenSick = fm.isSmokeWhenSick();

        // Validate input
        Set<String> newInterestNames = new HashSet<>(Optional.ofNullable(req.getInterests()).orElse(List.of()));
        if (!newInterestNames.isEmpty()) {
            List<InterestCategory> categories = interestCategoryRepository.findByNameIn(newInterestNames);
            Set<String> found = categories.stream().map(InterestCategory::getName).collect(Collectors.toSet());
            if (found.size() != newInterestNames.size()) {
                Set<String> invalid = new HashSet<>(newInterestNames);
                invalid.removeAll(found);
                throw new IllegalArgumentException("Invalid interests: " + invalid);
            }
        }

        Set<String> newTriggers = new HashSet<>(Optional.ofNullable(req.getTriggered()).orElse(List.of()));
        if (!ALLOWED_TRIGGERS.containsAll(newTriggers)) {
            Set<String> invalid = new HashSet<>(newTriggers);
            invalid.removeAll(ALLOWED_TRIGGERS);
            throw new IllegalArgumentException("Invalid triggers: " + invalid + ". Allowed: " + ALLOWED_TRIGGERS);
        }

        // Update entity
        fm.setSmokeAvgPerDay(req.getSmokeAvgPerDay());
        fm.setNumberOfYearsOfSmoking(req.getNumberOfYearsOfSmoking());
        fm.setCigarettesPerPackage(req.getCigarettesPerPackage());
        fm.setMinutesAfterWakingToSmoke(req.getMinutesAfterWakingToSmoke());
        fm.setSmokingInForbiddenPlaces(req.isSmokingInForbiddenPlaces());
        fm.setCigaretteHateToGiveUp(req.isCigaretteHateToGiveUp());
        fm.setMorningSmokingFrequency(req.isMorningSmokingFrequency());
        fm.setSmokeWhenSick(req.isSmokeWhenSick());
        fm.setMoneyPerPackage(req.getMoneyPerPackage());
//        fm.setEstimatedMoneySavedOnPlan(req.getEstimatedMoneySavedOnPlan());
        fm.setAmountOfNicotinePerCigarettes(req.getAmountOfNicotinePerCigarettes());
//        fm.setEstimatedNicotineIntakePerDay(req.getEstimatedNicotineIntakePerDay());
        fm.setInterests(new ArrayList<>(newInterestNames));
        fm.setTriggered(new ArrayList<>(newTriggers));

        formMetricRepository.save(fm);

        //Check thay đổi các field ảnh hưởng FTND
        boolean changedFtndInputs =
                oldSmokeAvgPerDay != req.getSmokeAvgPerDay() ||
                        oldMinutesAfterWakingToSmoke != req.getMinutesAfterWakingToSmoke() ||
                        oldNumberOfYearsOfSmoking != req.getNumberOfYearsOfSmoking() ||    // cai nay ko anh huong FTND nhung anh huong den thoi gian cua phase khi tao quitplan nen la dua vao luon
                        oldSmokingInForbiddenPlaces != req.isSmokingInForbiddenPlaces() ||
                        oldCigaretteHateToGiveUp != req.isCigaretteHateToGiveUp() ||
                        oldMorningSmokingFrequency != req.isMorningSmokingFrequency() ||
                        oldSmokeWhenSick != req.isSmokeWhenSick();

        int ftndScore = plan.getFtndScore();
        boolean alert = false;

        if (changedFtndInputs) {
            ftndScore = calculateFTNDScore(
                    req.getMinutesAfterWakingToSmoke(),
                    req.isSmokingInForbiddenPlaces(),
                    req.isCigaretteHateToGiveUp(),
                    req.getSmokeAvgPerDay(),
                    req.isMorningSmokingFrequency(),
                    req.isSmokeWhenSick()
            );
            plan.setFtndScore(ftndScore);
            quitPlanRepository.save(plan);
            alert = true;
        }

        // Return response
        UpdateFormMetricResponse resp = new UpdateFormMetricResponse();
        resp.setFormMetricDTO(formMetricMapper.toDTO(fm));
        resp.setAlert(alert);
        resp.setFntd_score(ftndScore);
        return resp;
    }


    private int calculateFTNDScore(int minutesAfterWakingToSmoke,
                                   boolean smokingInForbiddenPlaces,
                                   boolean cigaretteHateToGiveUp,
                                   int smokeAvgPerDay,
                                   boolean morningSmokingFrequency,
                                   boolean smokeWhenSick) {
        int score = 0;

        // 1) How soon after waking?
        if (minutesAfterWakingToSmoke <= 5) score += 3;
        else if (minutesAfterWakingToSmoke <= 30) score += 2;
        else if (minutesAfterWakingToSmoke <= 60) score += 1;

        // 2) Forbidden places
        if (smokingInForbiddenPlaces) score += 1;

        // 3) Hate to give up (First in the morning)
        if (cigaretteHateToGiveUp) score += 1;

        // 4) Cigarettes per day
        if (smokeAvgPerDay >= 31) score += 3;
        else if (smokeAvgPerDay >= 21) score += 2;
        else if (smokeAvgPerDay >= 11) score += 1;

        // 5) More frequently in morning
        if (morningSmokingFrequency) score += 1;

        // 6) Smoke when sick
        if (smokeWhenSick) score += 1;

        return score;
    }
}

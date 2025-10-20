package com.smartquit.smartquitiot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartquit.smartquitiot.dto.request.CreateQuitPlanInFirstLoginRequest;
import com.smartquit.smartquitiot.dto.response.PhaseDTO;
import com.smartquit.smartquitiot.dto.response.PhaseDetailResponseDTO;
import com.smartquit.smartquitiot.dto.response.PhaseResponse;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.PhaseDetailMissionStatus;
import com.smartquit.smartquitiot.enums.PhaseStatus;
import com.smartquit.smartquitiot.enums.QuitPlanStatus;
import com.smartquit.smartquitiot.repository.PhaseRepository;
import com.smartquit.smartquitiot.repository.QuitPlanRepository;
import com.smartquit.smartquitiot.repository.SystemPhaseConditionRepository;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.PhaseService;
import com.smartquit.smartquitiot.toolcalling.QuitPlanTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.smartquit.smartquitiot.toolcalling.QuitPlanTools.SYSTEM_PROMPT;

@Service
@Slf4j
@RequiredArgsConstructor
public class PhaseServiceImpl implements PhaseService {
    private final SystemPhaseConditionRepository  systemPhaseConditionRepository;
    private final QuitPlanTools quitPlanTools;
    private final ChatClient chatClient;
    private final PhaseRepository phaseRepository;
    private final AccountService  accountService;
    private final QuitPlanRepository quitPlanRepository;
    //nho lam cai schedule update status of PHASE
    @Override
    public PhaseDTO getCurrentPhaseAtHomePage() {
        Account account = accountService.getAuthenticatedAccount();
        QuitPlan plan = quitPlanRepository.findByMember_IdAndStatus(account.getMember().getId(), QuitPlanStatus.CREATED);
        if (plan == null) {
            plan = quitPlanRepository.findByMember_IdAndStatus(account.getMember().getId(), QuitPlanStatus.IN_PROGRESS);
        }
        if (plan == null) {
            throw new RuntimeException("Mission Plan Not Found at getCurrentPhaseAtHomePage tools");
        }

        LocalDate currentDate = LocalDate.now();
        Phase currentPhase = phaseRepository.findByStatusAndQuitPlan_Id(PhaseStatus.IN_PROGRESS,plan.getId())
                .orElseThrow(() -> new IllegalArgumentException("get current Phase not found at getCurrentPhaseAtHomePage"));

        int missionCompletedInCurrentPhaseDetail = 0;
        PhaseDetail currentPhaseDetail = null;
        for (PhaseDetail phaseDetail : currentPhase.getDetails()) {
            if (phaseDetail.getDate() != null && phaseDetail.getDate().isEqual(currentDate)) {
                currentPhaseDetail =  phaseDetail;
                for (PhaseDetailMission mission : phaseDetail.getPhaseDetailMissions()) {
                    if (mission.getStatus() == PhaseDetailMissionStatus.COMPLETED) {
                        missionCompletedInCurrentPhaseDetail++;
                    }
                }
            }
        }
        if(currentPhaseDetail == null){
            throw new RuntimeException("No active current Phase Detail found for current date at getCurrentPhaseAtHomePage");
        }

        PhaseDTO phaseDTO = new PhaseDTO();
        phaseDTO.setId(currentPhase.getId());
        phaseDTO.setName(currentPhase.getName());
        phaseDTO.setStartDate(currentPhase.getStartDate());
        phaseDTO.setEndDate(currentPhase.getEndDate());
        phaseDTO.setDurationDay(currentPhase.getDurationDays());
        phaseDTO.setReason(currentPhase.getReason());
        phaseDTO.setTotalMissions(currentPhase.getTotalMissions());
        phaseDTO.setCompletedMissions(currentPhase.getCompletedMissions());
        phaseDTO.setProgress(currentPhase.getProgress());
        phaseDTO.setCondition(currentPhase.getCondition());
        phaseDTO.setStartDateOfQuitPlan(plan.getStartDate());

        PhaseDetailResponseDTO  phaseDetailResponseDTO = new PhaseDetailResponseDTO();
        phaseDetailResponseDTO.setId(currentPhaseDetail.getId());
        phaseDetailResponseDTO.setName(currentPhaseDetail.getName());
        phaseDetailResponseDTO.setDate(currentPhaseDetail.getDate());
        phaseDetailResponseDTO.setDayIndex(currentPhaseDetail.getDayIndex());
        phaseDetailResponseDTO.setMissionCompleted(missionCompletedInCurrentPhaseDetail);
        phaseDetailResponseDTO.setTotalMission(currentPhaseDetail.getPhaseDetailMissions().size());
        phaseDTO.setCurrentPhaseDetail(phaseDetailResponseDTO);

        return phaseDTO;

    }

    @Override
    public PhaseResponse generatePhasesInFirstLogin(CreateQuitPlanInFirstLoginRequest req, int FTND, Account account) {
        //rules
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
                FTND,
                req.getStartDate());
        // response from ai
        PhaseResponse phaseResponse = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userInfo)
                .tools(quitPlanTools)
                .call()
                .entity(PhaseResponse.class);

        if (phaseResponse == null || phaseResponse.getPhases() == null || phaseResponse.getPhases().isEmpty()) {
            throw new IllegalStateException("AI did not return any phases");
        }

        return phaseResponse;
    }



    @Override
    public void savePhasesAndSystemPhaseCondition(PhaseResponse phaseResponse, QuitPlan quitPlan) {
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
            int duration = calcDurationInclusive(dto.getStartDate(), dto.getEndDate());
            phase.setDurationDays(duration);
         //   phase.setDurationDays(dto.getDurationDay());
            log.info("setDurationDays {}  and duration by AI {}", phase.getDurationDays(),dto
                    .getDurationDay());
            phase.setReason(dto.getReason());
            phase.setQuitPlan(quitPlan);
            phase.setStatus(PhaseStatus.CREATED);
            phase.setSystemPhaseCondition(allConditions.get(i)); // set theo thu tu trong condition, dung de xem nguon goc condition
            phase.setCondition(allConditions.get(i).getCondition()); // day moi la condition dung de kiem tra

            phaseRepository.save(phase);
        }
    }



    private int calculateAge(LocalDate dob) {
        return Period.between(dob, LocalDate.now()).getYears();
    }
    private int calcDurationInclusive(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Phase startDate/endDate must not be null");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1; // inclusive
        if (days <= 0) {
            throw new IllegalArgumentException("Phase endDate must be >= startDate (inclusive).");
        }
        if (days > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Duration too large.");
        }
        return (int) days;
    }
}

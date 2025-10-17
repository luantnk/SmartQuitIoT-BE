
package com.smartquit.smartquitiot.toolcalling;

import com.smartquit.smartquitiot.dto.response.InterestCategoryDTO;
import com.smartquit.smartquitiot.dto.response.PhaseDetailMissionToolDTO;
import com.smartquit.smartquitiot.dto.response.MissionTypeDTO;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Mission;
import com.smartquit.smartquitiot.entity.QuitPlan;
import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.enums.MissionStatus;
import com.smartquit.smartquitiot.enums.QuitPlanStatus;
import com.smartquit.smartquitiot.repository.QuitPlanRepository;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.MissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MissionTools {
    private final AccountService accountService;
    private final MissionService  missionService;
    private final QuitPlanRepository quitPlanRepository;

    public static final String SYS_PHASE = """
            You are a smoking cessation coach responsible for generating phase missions for a Quit Plan.
            
            CRITICAL RULES:
            - At the beginning, you MUST call the tool `getCandidateMissions` to fetch a list of mission candidates for the current phase.
            - You MUST ONLY choose missions from that tool response.
            - Do NOT create or modify mission codes or invent new ones.
            - Use mission id, code, name, and description exactly as provided.
            - If the tool returns an empty list, you MUST return an empty `missions` array for this phase.

            
            ASSIGNMENT RULES:
            - DO NOT repeat the same mission across different days in this phase.
            - Pick up to {MAX_PER_DAY} missions per day.
            - Return titles and descriptions, concise and contextual.
            
            SPECIAL RULES BY PHASE:
            
            ## For PREPARATION phase only:
            1. **PREP_LIST_TRIGGERS (Trigger Identification)**
               - MUST always appear on Day 1.
               - This task helps users identify personal smoking triggers early.
            
            2. **PREP_LEARN_NRT (Nicotine Replacement Therapy Learning)**
               - If `useNRT` == true and `PREP_LEARN_NRT` exists in the candidate list, include it early (preferably Day 1 or Day 2).
            
            3. **Priority when both exist**
               - `PREP_LIST_TRIGGERS` -> Day 1.
               - `PREP_LEARN_NRT` -> Day 2 (or immediately after triggers if space allows).
            
            4. **Context**
               - If `useNRT` == false → exclude `PREP_LEARN_NRT`.
               - Arrange other missions logically (motivational -> skill-building).
               - Distribute missions evenly across available days.
            
            ## For other phases:
            - Follow logical progression and avoid repetition.
            - Select missions contextually appropriate for the phase.
            
            """;

    @Tool(name = "getCandidateMissions",
            description = "To retrieve list mission candidates for the current phase.")
    public List<PhaseDetailMissionToolDTO> getCandidateMissions(@ToolParam(description = "Enum value of the target phase (PREPARATION, ONSET, PEAK_CRAVING, SUBSIDING, MAINTENANCE).")MissionPhase missionPhase) {
        Account account = accountService.getAuthenticatedAccount();
//        QuitPlan plan = quitPlanRepository.findByStatus(QuitPlanStatus.CREATED);
//        if(plan == null){
//            quitPlanRepository.findByStatus(QuitPlanStatus.IN_PROGRESS);
//        }
       QuitPlan plan = quitPlanRepository.findTopByOrderByCreatedAtDesc();
        if(plan == null){
            throw  new RuntimeException("Mission Plan Not Found at getCandidateMissions tools");
        }
        List<Mission> candidates = missionService.filterMissionsForPhase(
                plan, account,
                missionPhase,
                MissionStatus.ACTIVE
        );
        log.info("Filtered {} candidate missions for phase: {}", candidates.size(), missionPhase);
        log.info("Candidate codes: {}", candidates.stream()
                .map(Mission::getCode)
                .toList());

        List<PhaseDetailMissionToolDTO> missionToolDTOS = new ArrayList<>();
        for (Mission mission : candidates) {
            PhaseDetailMissionToolDTO missionToolDTO = new PhaseDetailMissionToolDTO();
            missionToolDTO.setId(mission.getId());
            missionToolDTO.setCode(mission.getCode());
            missionToolDTO.setName(mission.getName());
            missionToolDTO.setDescription(mission.getDescription());
            missionToolDTOS.add(missionToolDTO);
        }
        return missionToolDTOS;
    }

}

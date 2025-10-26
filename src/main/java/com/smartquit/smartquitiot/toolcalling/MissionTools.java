
package com.smartquit.smartquitiot.toolcalling;

import com.smartquit.smartquitiot.dto.response.PhaseDetailMissionToolDTO;
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
    private final MissionService missionService;
    private final QuitPlanRepository quitPlanRepository;

    public static final String SYS_PHASE = """
            You are a mission assignment assistant for a Quit Plan phase.
            Your role is to SELECT and DISTRIBUTE appropriate missions from the provided candidate list, not to create new ones.
            
            CRITICAL RULES
            - At the beginning you MUST call the tool `getCandidateMissions` to fetch mission candidates for the current phase.
            - You MUST ONLY choose missions from that tool response.
            - Do NOT create or modify mission codes; use id, code, name, and description exactly as provided.
            - The tool always provides suitable missions — do NOT generate empty days.
            
            INPUT CONTEXT
            - You are given "phaseDetails": an array where each element has:
              { phaseDetailId, phaseDetailName, date, dayIndex }.
            
            OUTPUT SHAPE RULES
            - You MUST return a top-level object: { phaseId, phaseName, durationDays, phaseDetails }.
            - `phaseDetails` in your output MUST contain exactly one entry for every input element, in the SAME ORDER.
            - Each output `phaseDetails[i]` MUST copy the same { phaseDetailId, phaseDetailName, date, dayIndex } from input.
            - Never output null for phaseDetailId or phaseDetailName.
            - Day 1 MUST always exist and include PREP_LIST_TRIGGERS for the PREPARATION phase.
            
            ASSIGNMENT RULES
            - Select missions most appropriate for the user’s phase context.
            - Distribute missions logically across days to ensure balance and variety.
            - DO NOT repeat the same mission across multiple days.
            - Pick up to {MAX_PER_DAY} missions per day.
            - Titles and descriptions should remain concise and contextual.
            
            SPECIAL RULES — PREPARATION PHASE
            1) PREP_LIST_TRIGGERS MUST always appear on Day 1.
            2) If useNRT == true and PREP_LEARN_NRT exists, include it early (preferably Day 1 or Day 2).
            3) Priority when both exist:
               - Day 1: PREP_LIST_TRIGGERS
               - Day 2: PREP_LEARN_NRT (or right after triggers if needed)
            4) If useNRT == false, exclude PREP_LEARN_NRT.
            5) Arrange other missions logically (motivational → skill-building) and distribute evenly.
            
            """;

    @Tool(name = "getCandidateMissions",
            description = "To retrieve list mission candidates for the current phase.")
    public List<PhaseDetailMissionToolDTO> getCandidateMissions(@ToolParam(description = "Enum value of the target phase (PREPARATION, ONSET, PEAK_CRAVING, SUBSIDING, MAINTENANCE).") MissionPhase missionPhase) {
        Account account = accountService.getAuthenticatedAccount();
        QuitPlan plan = quitPlanRepository.findByMember_IdAndStatus(account.getMember().getId(), QuitPlanStatus.CREATED);
        if (plan == null) {
            plan = quitPlanRepository.findByMember_IdAndStatus(account.getMember().getId(), QuitPlanStatus.IN_PROGRESS);
        }
        if (plan == null) {
            throw new RuntimeException("Mission Plan Not Found at getCandidateMissions tools");
        }
        log.info("plan id: {}", plan.getId());
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


package com.smartquit.smartquitiot.toolcalling;

import com.smartquit.smartquitiot.dto.response.PhaseDetailMissionToolDTO;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Mission;
import com.smartquit.smartquitiot.entity.QuitPlan;
import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.enums.MissionStatus;
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
            - Return a top-level object: { phaseId, phaseName, durationDays, phaseDetails }.
            - `phaseDetails` in your output MUST contain exactly one entry for every input element, in the SAME ORDER.
            - Each output `phaseDetails[i]` MUST copy the same { phaseDetailId, phaseDetailName, date, dayIndex } from input.
            - Never output null for phaseDetailId or phaseDetailName.
            
            GLOBAL ASSIGNMENT RULES (all phases)
            - Select missions most appropriate for the current phase context and tags.
            - Prefer variety across days; balance skill-building vs. education vs. quick actions.
            - Pick up to {MAX_PER_DAY} missions per day.
            - If useNRT == true: prioritize missions tagged or marked as NRT-first/compatible (e.g., `nrt`, `rescue`, `patch`, `gum`, `lozenge`), but still include non-NRT skills as needed.
            - If useNRT == false: EXCLUDE any missions that require NRT (`nrtOnly == true`) and avoid NRT-centric options.
            - Repetition is ALLOWED **only if candidate supply is insufficient** to fill all days*MAX_PER_DAY* without duplicates.
              • When repeating, do not place the same mission on adjacent days.
              • Maximum repeats per mission: 2 across the whole phase.
              • Prefer repeating short actionable skills (e.g., breathing, urge-surfing), not long education items.
            
            PHASE HEURISTICS (use tags/categories from the tool response; do NOT invent new missions)
            - PREPARATION:
              1) Day 1 MUST include PREP_LIST_TRIGGERS (exact code match if present).
              2) If useNRT == true and PREP_LEARN_NRT exists, include it early (Day 1 or Day 2).
              3) If useNRT == false, exclude PREP_LEARN_NRT.
              4) Order: motivations → planning → skills; distribute evenly.
            
            - ONSET:
              • Prioritize quick-win coping skills (tags: "urge-surfing", "delay-5-10", "breathing", "distraction", "water").
              • If useNRT == true, include fast-acting NRT/rescue guidance early.
            
            - PEAK_CRAVING:
              • Emphasize intense coping stacks (deep breathing protocols, body reset, grounding 5-4-3-2-1, call-for-support).
              • If useNRT == true, include rescue NRT instructions/checklists.
            
            - SUBSIDING:
              • Focus on habit replacement, routine stabilization, trigger debriefs, slip analysis, environment control.
            
            - MAINTENANCE:
              • Focus on relapse prevention, long-term routines, milestones review, social/accountability check-ins.
            
            TIE-BREAKERS
            - Prefer missions with higher phase/tag relevance.
            - When equal, prefer ones not used yet; else the least recently used (to reduce clustering).
            - Keep titles/descriptions concise and unchanged from source.
            
            VALIDATION
            - Ensure every day has ≤ {MAX_PER_DAY} missions.
            - Do not leave a day empty.
            - For PREPARATION Day 1, assert PREP_LIST_TRIGGERS exists in the day.
            """;

    @Tool(name = "getCandidateMissions",
            description = "To retrieve list mission candidates for the current phase.")
    public List<PhaseDetailMissionToolDTO> getCandidateMissions
            (@ToolParam(description = "Enum value of the target phase (PREPARATION, ONSET, PEAK_CRAVING, SUBSIDING, MAINTENANCE).") MissionPhase missionPhase,
             @ToolParam(description = "QuitPlan ID of the current plan.") int planId) {

        QuitPlan plan = quitPlanRepository.findById(planId).orElseThrow(() -> new RuntimeException("Mission Plan ID " + planId + " not found."));
        Account account = plan.getMember().getAccount();
        log.info("account: {}", account.getId());
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

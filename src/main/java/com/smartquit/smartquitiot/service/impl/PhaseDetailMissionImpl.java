package com.smartquit.smartquitiot.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartquit.smartquitiot.dto.response.PhaseBatchMissionsResponse;
import com.smartquit.smartquitiot.dto.response.PhaseDetailMissionPlanToolDTO;
import com.smartquit.smartquitiot.dto.response.PhaseDetailPlanToolDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.repository.MissionRepository;
import com.smartquit.smartquitiot.repository.PhaseDetailMissionRepository;
import com.smartquit.smartquitiot.repository.PhaseDetailRepository;
import com.smartquit.smartquitiot.repository.PhaseRepository;
import com.smartquit.smartquitiot.service.PhaseDetailMissionService;
import com.smartquit.smartquitiot.toolcalling.MissionTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.smartquit.smartquitiot.toolcalling.MissionTools.SYS_PHASE;

@Service
@Slf4j
@RequiredArgsConstructor
public class PhaseDetailMissionImpl implements PhaseDetailMissionService {
    private final PhaseDetailRepository  phaseDetailRepository;
    private final PhaseDetailMissionRepository phaseDetailMissionRepository;
    private final MissionRepository missionRepository;
    private final PhaseRepository phaseRepository;
    private final ChatClient  chatClient;
    private final MissionTools  missionTools;

    @Override
    @Transactional
    public PhaseBatchMissionsResponse generatePhaseDetailMissionsForPhase(List<PhaseDetail> preparedDetails , QuitPlan plan, int maxPerDay, String phaseName, MissionPhase missionPhase) {

        Phase phase = phaseRepository.findByQuitPlan_IdAndName(plan.getId(),phaseName)
                .orElseThrow(() -> new RuntimeException("phase not found at generatePhaseMissionsBatch"));
        PhaseBatchMissionsResponse ai = callAiForPhaseBatch(
                phase,
                plan,
                preparedDetails,
                maxPerDay,
                missionPhase
        );

        savePhaseDetailMissionsForPhase(ai);
        return ai;

    }

    private PhaseBatchMissionsResponse callAiForPhaseBatch(
            Phase phase,
            QuitPlan plan,
            List<PhaseDetail> phaseDetails,
            int maxPerDay,
            MissionPhase missionPhase
    ) {
        String sys = SYS_PHASE.replace("{MAX_PER_DAY}", String.valueOf(maxPerDay));

        // context user
        var userInfo = Map.of(
                "phaseId", phase.getId(),
                "phaseName", phase.getName(),
                "durationDays", phase.getDurationDays(),
                "missionPhase", missionPhase.name(),
                "phaseDetails", phaseDetails.stream()
                        .map(d -> Map.of(
                                "phaseDetailId", d.getId(),
                                "phaseDetailName",d.getName(),
                                "date", d.getDate() != null ? d.getDate().toString() : null,
                                "dayIndex", d.getDayIndex()
                        ))
                        .toList(),
                "FTND", plan.getFtndScore(),
                "smokeAvgPerDay", plan.getFormMetric().getSmokeAvgPerDay(),
                "useNRT", plan.isUseNRT()
        );

        return chatClient.prompt()
                .system(sys)
                .tools(missionTools)
                .user(toJson(userInfo))
                .call()
                .entity(PhaseBatchMissionsResponse.class);
    }


    private String toJson(Object o) {
        try {
            return new ObjectMapper().writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    @Transactional
    public void savePhaseDetailMissionsForPhase(PhaseBatchMissionsResponse resp) {
        if (resp == null || resp.getItems() == null) {
            log.warn("AI response is null or items null -> nothing to persist");
            return;
        }

        for (PhaseDetailPlanToolDTO day : resp.getItems()) {
            Integer phaseDetailId = day.getPhaseDetailId();
            if (phaseDetailId == null) {
                log.warn("Skip a day because phaseDetailId is null");
                continue;
            }

            PhaseDetail phaseDetail = phaseDetailRepository.findById(phaseDetailId)
                    .orElseThrow(() -> new IllegalStateException("PhaseDetail not found: " + phaseDetailId));

            if (day.getMissions() == null || day.getMissions().isEmpty()) {
                log.info("Day {} has no missions from AI. Kept empty.", phaseDetailId);
                continue;
            }

            List<PhaseDetailMission> toSave = new ArrayList<>();
            for (PhaseDetailMissionPlanToolDTO m : day.getMissions()) {
                Mission mission = null;
                if (m.getId() > 0) {
                    mission = missionRepository.findById(m.getId()).orElse(null);
                }
                if (mission == null && m.getCode() != null) {
                    mission = missionRepository.findByCode(m.getCode()).orElse(null);
                }

                if (mission == null) {
                    log.warn("Mission not found (id={}, code={}), skip", m.getId(), m.getCode());
                    continue;
                }

                PhaseDetailMission entity = new PhaseDetailMission();
                entity.setMission(mission);
                entity.setPhaseDetail(phaseDetail);
                entity.setCode(mission.getCode());
                entity.setName(mission.getName());
                entity.setDescription(mission.getDescription());

                toSave.add(entity);
            }

            if (!toSave.isEmpty()) {
                phaseDetailMissionRepository.saveAll(toSave);
                log.info("Saved {} PhaseDetailMission for phaseDetailId={}", toSave.size(), phaseDetailId);
            } else {
                log.info("No valid missions to save for phaseDetailId={}", phaseDetailId);
            }
        }
    }
}

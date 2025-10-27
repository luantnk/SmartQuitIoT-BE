package com.smartquit.smartquitiot.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartquit.smartquitiot.dto.request.AddAchievementRequest;
import com.smartquit.smartquitiot.dto.request.CompleteMissionRequest;
import com.smartquit.smartquitiot.dto.response.*;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.enums.PhaseDetailMissionStatus;
import com.smartquit.smartquitiot.enums.PhaseStatus;
import com.smartquit.smartquitiot.enums.QuitPlanStatus;
import com.smartquit.smartquitiot.mapper.QuitPlanMapper;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.MemberAchievementService;
import com.smartquit.smartquitiot.service.PhaseDetailMissionService;
import com.smartquit.smartquitiot.toolcalling.MissionTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.smartquit.smartquitiot.toolcalling.MissionTools.SYS_PHASE;

@Service
@Slf4j
@RequiredArgsConstructor
public class PhaseDetailMissionServiceImpl implements PhaseDetailMissionService {
    private final PhaseDetailRepository  phaseDetailRepository;
    private final PhaseDetailMissionRepository phaseDetailMissionRepository;
    private final MissionRepository missionRepository;
    private final PhaseRepository phaseRepository;
    private final ChatClient  chatClient;
    private final MissionTools  missionTools;
    private final QuitPlanMapper quitPlanMapper;
    private final AccountService  accountService;
    private final QuitPlanRepository quitPlanRepository;
    private final FormMetricRepository formMetricRepository;
    private final MetricRepository metricRepository;
    private final MemberAchievementService memberAchievementService;

    @Override
    @Transactional
    public QuitPlanResponse completePhaseDetailMission(CompleteMissionRequest req) {
        PhaseDetailMission phaseDetailMission = phaseDetailMissionRepository.findById(req.getPhaseDetailMissionId())
                .orElseThrow(() -> new IllegalArgumentException("PhaseDetailMission not found: " + req.getPhaseDetailMissionId()));
        LocalDate currentDate = LocalDate.now();

        if(!phaseDetailMission.getPhaseDetail().getDate().equals(currentDate)) {
            throw new IllegalStateException("Phase detail mission id is not today");
        }

        Account account = accountService.getAuthenticatedAccount();
        Metric metric = metricRepository.findByMemberId(account.getMember().getId())
                .orElseThrow(() ->  new IllegalArgumentException("Metric not found: " + account.getMember().getId()));

        if(!phaseDetailMission.getStatus().equals(PhaseDetailMissionStatus.COMPLETED)){
            phaseDetailMission.setCompletedAt(LocalDateTime.now());
            phaseDetailMission.setStatus(PhaseDetailMissionStatus.COMPLETED);
            Phase phase = phaseRepository.findById(req.getPhaseId()) .orElseThrow(() -> new IllegalArgumentException("Phase not found: " + req.getPhaseDetailMissionId()));
            phase.setCompletedMissions(phase.getCompletedMissions() + 1);
            phase.setProgress(calculateProgress(phase));
            phaseDetailMissionRepository.save(phaseDetailMission);
            Phase newPhase = phaseRepository.save(phase);

            //set trigger for from metric
            if(phaseDetailMission.getCode().equals("PREP_LIST_TRIGGERS")){
                FormMetric formMetric = phase.getQuitPlan().getFormMetric();
                formMetric.setTriggered(req.getTriggered());
                formMetricRepository.save(formMetric);
            }

            int completedMissionInPhaseDetail = 0;
            for (PhaseDetail phaseDetail : newPhase.getDetails()) {
                if (phaseDetail.getDate() != null && phaseDetail.getDate().isEqual(currentDate)) {
                    for (PhaseDetailMission mission : phaseDetail.getPhaseDetailMissions()) {
                        if(mission.getStatus().equals(PhaseDetailMissionStatus.COMPLETED)){
                            completedMissionInPhaseDetail++;
                        }
                    }
                    if(completedMissionInPhaseDetail == phaseDetail.getPhaseDetailMissions().size()){
                        metric.setCompleted_all_mission_in_day(metric.getCompleted_all_mission_in_day() + 1);
                    }
                }
            }

            metric.setTotal_mission_completed(metric.getTotal_mission_completed() + 1);
            metricRepository.save(metric);

            AddAchievementRequest addAchievementRequestTotalMissionCompleted = new  AddAchievementRequest();
            addAchievementRequestTotalMissionCompleted.setField("total_mission_completed");
            memberAchievementService.addMemberAchievement(addAchievementRequestTotalMissionCompleted).orElse(null);

            AddAchievementRequest addAchievementRequestAllMissionInDay = new  AddAchievementRequest();
            addAchievementRequestAllMissionInDay.setField("completed_all_mission_in_day");
            memberAchievementService.addMemberAchievement(addAchievementRequestAllMissionInDay).orElse(null);


            return quitPlanMapper.toResponse(phase.getQuitPlan());

        }else{
            throw new IllegalStateException("Phase detail mission has been completed");
        }
    }

    @Override
    public MissionTodayResponse getListMissionToday() {

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

        MissionTodayResponse missionTodayResponse = new MissionTodayResponse();
        List<PhaseDetailMissionResponseDTO> phaseDetailMissionResponseDTOS = new ArrayList<>();
        for (PhaseDetail phaseDetail : currentPhase.getDetails()) {
            if (phaseDetail.getDate() != null && phaseDetail.getDate().isEqual(currentDate)) {
                for (PhaseDetailMission mission : phaseDetail.getPhaseDetailMissions()) {
                    PhaseDetailMissionResponseDTO  phaseDetailMissionResponseDTO = new PhaseDetailMissionResponseDTO();
                    phaseDetailMissionResponseDTO.setId(mission.getId());
                    phaseDetailMissionResponseDTO.setStatus(mission.getStatus());
                    phaseDetailMissionResponseDTO.setName(mission.getName());
                    phaseDetailMissionResponseDTO.setDescription(mission.getDescription());
                    phaseDetailMissionResponseDTO.setCode(mission.getCode());
                    phaseDetailMissionResponseDTO.setCompletedAt(mission.getCompletedAt());
                    phaseDetailMissionResponseDTOS.add(phaseDetailMissionResponseDTO);
                }
            }
        }
        if(phaseDetailMissionResponseDTOS.isEmpty()){
            throw new IllegalArgumentException("List Phase Detail Mission to day is empty at getListMissionToday");
        }
        missionTodayResponse.setPhaseDetailMissionResponseDTOS(phaseDetailMissionResponseDTOS);
        missionTodayResponse.setPhaseId(currentPhase.getId());

        return missionTodayResponse;
    }

    // api này ko có dùng
    @Override
    public MissionTodayResponse completePhaseDetailMissionAtHomePage(CompleteMissionRequest request) {

        PhaseDetailMission phaseDetailMission = phaseDetailMissionRepository.findById(request.getPhaseDetailMissionId())
                .orElseThrow(() -> new IllegalArgumentException("PhaseDetailMission not found: " + request.getPhaseDetailMissionId()));

        LocalDate currentDate = LocalDate.now();
        if(!phaseDetailMission.getPhaseDetail().getDate().equals(currentDate)) {
            throw new IllegalStateException("Phase detail mission id is not today");
        }
        if(!phaseDetailMission.getStatus().equals(PhaseDetailMissionStatus.COMPLETED)){
            phaseDetailMission.setCompletedAt(LocalDateTime.now());
            phaseDetailMission.setStatus(PhaseDetailMissionStatus.COMPLETED);
            Phase phase = phaseRepository.findById(request.getPhaseId()) .orElseThrow(() -> new IllegalArgumentException("Phase not found: " + request.getPhaseDetailMissionId()));
            phase.setCompletedMissions(phase.getCompletedMissions() + 1);
            phase.setProgress(calculateProgress(phase));

            phaseDetailMissionRepository.save(phaseDetailMission);
            Phase newPhase = phaseRepository.save(phase);

            MissionTodayResponse missionTodayResponse = new MissionTodayResponse();
            List<PhaseDetailMissionResponseDTO> phaseDetailMissionResponseDTOS = new ArrayList<>();
            int completedMissionInPhaseDetail = 0;
            for (PhaseDetail phaseDetail : newPhase.getDetails()) {
                if (phaseDetail.getDate() != null && phaseDetail.getDate().isEqual(currentDate)) {
                    for (PhaseDetailMission mission : phaseDetail.getPhaseDetailMissions()) {
                        PhaseDetailMissionResponseDTO  phaseDetailMissionResponseDTO = new PhaseDetailMissionResponseDTO();
                        phaseDetailMissionResponseDTO.setId(mission.getId());
                        phaseDetailMissionResponseDTO.setStatus(mission.getStatus());
                        phaseDetailMissionResponseDTO.setName(mission.getName());
                        phaseDetailMissionResponseDTO.setDescription(mission.getDescription());
                        phaseDetailMissionResponseDTO.setCode(mission.getCode());
                        phaseDetailMissionResponseDTO.setCompletedAt(mission.getCompletedAt());
                        phaseDetailMissionResponseDTOS.add(phaseDetailMissionResponseDTO);
                        if(mission.getStatus().equals(PhaseDetailMissionStatus.COMPLETED)){
                            completedMissionInPhaseDetail++;
                        }
                    }
                    if(completedMissionInPhaseDetail == phaseDetail.getPhaseDetailMissions().size()){
                        missionTodayResponse.setPopup("Congratulations! All missions for today are complete. You're crushing it!");
                    }
                }
            }
            if(phaseDetailMissionResponseDTOS.isEmpty()){
                throw new IllegalArgumentException("List Phase Detail Mission to day is empty at completePhaseDetailMissionAtHomePage");
            }
            missionTodayResponse.setPhaseDetailMissionResponseDTOS(phaseDetailMissionResponseDTOS);
            missionTodayResponse.setPhaseId(newPhase.getId());


            return missionTodayResponse;

        }else{
            throw new IllegalStateException("Phase detail mission has been completed");
        }
    }

    private BigDecimal calculateProgress(Phase phase) {
        int total = phase.getTotalMissions();
        int done  = phase.getCompletedMissions();

        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal progress = BigDecimal.valueOf(done)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        if (progress.compareTo(BigDecimal.ZERO) < 0)  return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (progress.compareTo(BigDecimal.valueOf(100)) > 0) return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);

        return progress;
    }




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
//        if (ai != null && ai.getItems() != null && !ai.getItems().isEmpty()) {
//            for (int i = 0; i < ai.getItems().size() && i < preparedDetails.size(); i++) {
//                var dto = ai.getItems().get(i);
//                PhaseDetail pd = preparedDetails.get(i);
//                if (dto.getPhaseDetailId() == null) {
//                    log.warn("duma getPhaseDetailId null roi");
//                    dto.setPhaseDetailId(pd.getId());
//                    dto.setPhaseDetailName(pd.getName());
//                }
//            }
//        }

       int totalMissions = savePhaseDetailMissionsForPhase(ai);
       phase.setTotalMissions(totalMissions);
       phase.setCompletedMissions(0);
       phase.setProgress(BigDecimal.ZERO);
       phaseRepository.save(phase);
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
    public int savePhaseDetailMissionsForPhase(PhaseBatchMissionsResponse resp) {
        if (resp == null || resp.getPhaseDetails() == null) {
            log.warn("AI response is null or items null -> nothing to persist");
            return 0;
        }
        int totalSaved = 0;
        for (PhaseDetailPlanToolDTO day : resp.getPhaseDetails()) {

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
                entity.setStatus(PhaseDetailMissionStatus.INCOMPLETED);
                entity.setDescription(mission.getDescription());

                toSave.add(entity);
            }

            if (!toSave.isEmpty()) {
                phaseDetailMissionRepository.saveAll(toSave);
                totalSaved +=  toSave.size();
                log.info("Saved {} PhaseDetailMission for phaseDetailId={}", toSave.size(), phaseDetailId);
            } else {
                log.info("No valid missions to save for phaseDetailId={}", phaseDetailId);
            }
        }
//        if (totalSaved <= 0) {
//            throw new RuntimeException(
//                    "No PhaseDetailMission saved at savePhaseDetailMissionsForPhase — possible causes: null phaseDetailId or invalid mission mapping. Phase: "
//                            + resp.getPhaseName()
//            );
//        }

        return totalSaved;
    }


}

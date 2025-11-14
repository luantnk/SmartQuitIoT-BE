package com.smartquit.smartquitiot.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartquit.smartquitiot.dto.request.AddAchievementRequest;
import com.smartquit.smartquitiot.dto.request.CompleteMissionRequest;
import com.smartquit.smartquitiot.dto.response.*;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.*;
import com.smartquit.smartquitiot.mapper.QuitPlanMapper;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.*;
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
    private final PhaseDetailRepository phaseDetailRepository;
    private final PhaseDetailMissionRepository phaseDetailMissionRepository;
    private final MissionRepository missionRepository;
    private final PhaseRepository phaseRepository;
    private final ChatClient chatClient;
    private final MissionTools missionTools;
    private final QuitPlanMapper quitPlanMapper;
    private final AccountService accountService;
    private final QuitPlanRepository quitPlanRepository;
    private final FormMetricRepository formMetricRepository;
    private final MetricRepository metricRepository;
    private final MemberAchievementService memberAchievementService;
    private final NotificationService notificationService;


    @Override
    @Transactional
    public QuitPlanResponse completePhaseDetailMission(CompleteMissionRequest req) {
        PhaseDetailMission phaseDetailMission = phaseDetailMissionRepository.findById(req.getPhaseDetailMissionId())
                .orElseThrow(() -> new IllegalArgumentException("PhaseDetailMission not found: " + req.getPhaseDetailMissionId()));
        Phase phase = phaseRepository.findById(req.getPhaseId())
                .orElseThrow(() -> new IllegalArgumentException("Phase not found: " + req.getPhaseId()));

        LocalDate currentDate = LocalDate.now();
        LocalDate missionDate = phaseDetailMission.getPhaseDetail().getDate();
        LocalDate phaseStart = phase.getStartDate();

        if (missionDate == null) {
            throw new IllegalStateException("PhaseDetail date is null");
        }
        if (phaseStart != null && missionDate.isBefore(phaseStart)) {
            throw new IllegalStateException("Mission date is before phase start");
        }
        if (missionDate.isAfter(currentDate)) {
            throw new IllegalStateException("Cannot complete a future mission");
        }
//        if (!phaseDetailMission.getPhaseDetail().getDate().equals(currentDate)) {
//            throw new IllegalStateException("Phase detail mission id is not today");
//        }

        Account account = accountService.getAuthenticatedAccount();
        Metric metric = metricRepository.findByMemberId(account.getMember().getId())
                .orElseThrow(() -> new IllegalArgumentException("Metric not found: " + account.getMember().getId()));
        if (phaseDetailMission.getStatus() == PhaseDetailMissionStatus.COMPLETED) {
            throw new IllegalStateException("Phase detail mission has been completed");
        }

        phaseDetailMission.setCompletedAt(LocalDateTime.now());
        phaseDetailMission.setStatus(PhaseDetailMissionStatus.COMPLETED);
        phaseDetailMissionRepository.save(phaseDetailMission);
        //  Phase phase = phaseRepository.findById(req.getPhaseId()).orElseThrow(() -> new IllegalArgumentException("Phase not found: " + req.getPhaseDetailMissionId()));
        int total = 0, done = 0;
        for (PhaseDetail d : phase.getDetails()) {
            for (PhaseDetailMission m : d.getPhaseDetailMissions()) {
                total++;
                if (m.getStatus() == PhaseDetailMissionStatus.COMPLETED) done++;
            }
        }
        phase.setCompletedMissions(done);
        phase.setProgress(calculateProgress(phase)); // your existing method
        Phase newPhase = phaseRepository.save(phase);

        //set trigger for from metric
        if (phaseDetailMission.getCode().equals("PREP_LIST_TRIGGERS")) {
            FormMetric formMetric = phase.getQuitPlan().getFormMetric();
            formMetric.setTriggered(req.getTriggered());
            formMetricRepository.save(formMetric);
        }

        PhaseDetail missionDayDetail = phaseDetailMission.getPhaseDetail();
        boolean allDoneThatDay = missionDayDetail.getPhaseDetailMissions()
                .stream().allMatch(m -> m.getStatus() == PhaseDetailMissionStatus.COMPLETED);

        if (allDoneThatDay) {
            metric.setCompleted_all_mission_in_day(metric.getCompleted_all_mission_in_day() + 1);

            String url = "notifications/phase/" + req.getPhaseId();
            String deepLink = "smartquit://phase/" + req.getPhaseId();

            notificationService.saveAndPublish(
                    account.getMember(),
                    NotificationType.MISSION,
                    "Done all missions for " + missionDate + "!",
                    "You completed every mission on " + missionDate + ". Keep the streak alive!",
                    "https://res.cloudinary.com/dsuxhxkya/image/upload/v1762324029/logo_wxhjsa.png",
                    url,
                    deepLink
            );
        }

        metric.setTotal_mission_completed(metric.getTotal_mission_completed() + 1);
        metricRepository.save(metric);

        // Achievement checks
        AddAchievementRequest reqTotal = new AddAchievementRequest();
        reqTotal.setField("total_mission_completed");
        memberAchievementService.addMemberAchievement(reqTotal).orElse(null);

        AddAchievementRequest reqAllInDay = new AddAchievementRequest();
        reqAllInDay.setField("completed_all_mission_in_day");
        memberAchievementService.addMemberAchievement(reqAllInDay).orElse(null);

        return quitPlanMapper.toResponse(newPhase.getQuitPlan());

    }

    @Override
    public MissionTodayResponse getListMissionToday() {

        Account account = accountService.getAuthenticatedAccount();
        QuitPlan plan = quitPlanRepository.findByMember_IdAndIsActiveTrue(account.getMember().getId());
        if (plan == null) {
            throw new RuntimeException("Mission Plan Not Found at getCurrentPhaseAtHomePage tools");
        }

        LocalDate currentDate = LocalDate.now();
        Phase currentPhase = phaseRepository.findByStatusAndQuitPlan_Id(PhaseStatus.IN_PROGRESS, plan.getId())
                .orElseThrow(() -> new IllegalArgumentException("get current Phase not found at getCurrentPhaseAtHomePage"));

        MissionTodayResponse missionTodayResponse = new MissionTodayResponse();
        List<PhaseDetailMissionResponseDTO> phaseDetailMissionResponseDTOS = new ArrayList<>();
        for (PhaseDetail phaseDetail : currentPhase.getDetails()) {
            if (phaseDetail.getDate() != null && phaseDetail.getDate().isEqual(currentDate)) {
                for (PhaseDetailMission mission : phaseDetail.getPhaseDetailMissions()) {
                    PhaseDetailMissionResponseDTO phaseDetailMissionResponseDTO = new PhaseDetailMissionResponseDTO();
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
        if (phaseDetailMissionResponseDTOS.isEmpty()) {
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
        if (!phaseDetailMission.getPhaseDetail().getDate().equals(currentDate)) {
            throw new IllegalStateException("Phase detail mission id is not today");
        }
        if (!phaseDetailMission.getStatus().equals(PhaseDetailMissionStatus.COMPLETED)) {
            phaseDetailMission.setCompletedAt(LocalDateTime.now());
            phaseDetailMission.setStatus(PhaseDetailMissionStatus.COMPLETED);
            Phase phase = phaseRepository.findById(request.getPhaseId()).orElseThrow(() -> new IllegalArgumentException("Phase not found: " + request.getPhaseDetailMissionId()));
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
                        PhaseDetailMissionResponseDTO phaseDetailMissionResponseDTO = new PhaseDetailMissionResponseDTO();
                        phaseDetailMissionResponseDTO.setId(mission.getId());
                        phaseDetailMissionResponseDTO.setStatus(mission.getStatus());
                        phaseDetailMissionResponseDTO.setName(mission.getName());
                        phaseDetailMissionResponseDTO.setDescription(mission.getDescription());
                        phaseDetailMissionResponseDTO.setCode(mission.getCode());
                        phaseDetailMissionResponseDTO.setCompletedAt(mission.getCompletedAt());
                        phaseDetailMissionResponseDTOS.add(phaseDetailMissionResponseDTO);
                        if (mission.getStatus().equals(PhaseDetailMissionStatus.COMPLETED)) {
                            completedMissionInPhaseDetail++;
                        }
                    }
                    if (completedMissionInPhaseDetail == phaseDetail.getPhaseDetailMissions().size()) {
                        missionTodayResponse.setPopup("Congratulations! All missions for today are complete. You're crushing it!");
                    }
                }
            }
            if (phaseDetailMissionResponseDTOS.isEmpty()) {
                throw new IllegalArgumentException("List Phase Detail Mission to day is empty at completePhaseDetailMissionAtHomePage");
            }
            missionTodayResponse.setPhaseDetailMissionResponseDTOS(phaseDetailMissionResponseDTOS);
            missionTodayResponse.setPhaseId(newPhase.getId());


            return missionTodayResponse;

        } else {
            throw new IllegalStateException("Phase detail mission has been completed");
        }
    }

    private BigDecimal calculateProgress(Phase phase) {
        int total = phase.getTotalMissions();
        int done = phase.getCompletedMissions();

        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal progress = BigDecimal.valueOf(done)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        if (progress.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (progress.compareTo(BigDecimal.valueOf(100)) > 0)
            return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);

        return progress;
    }


    @Override
    @Transactional
    public PhaseBatchMissionsResponse generatePhaseDetailMissionsForPhase(List<PhaseDetail> preparedDetails, QuitPlan plan, int maxPerDay, String phaseName, MissionPhase missionPhase) {
        log.info("generatePhaseDetailMissionsForPhase");
        Phase phase = phaseRepository.findByQuitPlan_IdAndName(plan.getId(), phaseName)
                .orElseThrow(() -> new RuntimeException("phase not found at generatePhaseMissionsBatch"));
        PhaseBatchMissionsResponse ai = callAiForPhaseBatch(
                phase,
                plan,
                preparedDetails,
                maxPerDay,
                missionPhase
        );

        int totalMissions = savePhaseDetailMissionsForPhase(ai);
        phase.setTotalMissions(totalMissions);
        phase.setCompletedMissions(0);
        phase.setProgress(BigDecimal.ZERO);
        phaseRepository.save(phase);
        return ai;

    }

    @Override
    @Transactional
    public PhaseBatchMissionsResponse generatePhaseDetailMissionsForPhaseInScheduler(Phase phase, List<PhaseDetail> preparedDetails, QuitPlan plan, int maxPerDay, String phaseName, MissionPhase missionPhase) {
        log.info("generatePhaseDetailMissionsForPhaseInScheduler");
        PhaseBatchMissionsResponse ai = callAiForPhaseBatch(
                phase,
                plan,
                preparedDetails,
                maxPerDay,
                missionPhase
        );

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
        log.info("callAiForPhaseBatch");
        // context user
        var userInfo = Map.of(
                "phaseId", phase.getId(),
                "phaseName", phase.getName(),
                "durationDays", phase.getDurationDays(),
                "missionPhase", missionPhase.name(),
                "phaseDetails", phaseDetails.stream()
                        .map(d -> Map.of(
                                "phaseDetailId", d.getId(),
                                "phaseDetailName", d.getName(),
                                "date", d.getDate() != null ? d.getDate().toString() : null,
                                "dayIndex", d.getDayIndex()
                        ))
                        .toList(),
                "FTND", plan.getFtndScore(),
                "smokeAvgPerDay", plan.getFormMetric().getSmokeAvgPerDay(),
                "useNRT", plan.isUseNRT(),
                "planId", plan.getId()
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
                totalSaved += toSave.size();
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

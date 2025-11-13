package com.smartquit.smartquitiot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartquit.smartquitiot.dto.request.CreateQuitPlanInFirstLoginRequest;
import com.smartquit.smartquitiot.dto.request.RedoPhaseRequest;
import com.smartquit.smartquitiot.dto.response.PhaseDTO;
import com.smartquit.smartquitiot.dto.response.PhaseDetailResponseDTO;
import com.smartquit.smartquitiot.dto.response.PhaseResponse;
import com.smartquit.smartquitiot.dto.response.QuitPlanResponse;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.*;
import com.smartquit.smartquitiot.mapper.QuitPlanMapper;
import com.smartquit.smartquitiot.repository.PhaseRepository;
import com.smartquit.smartquitiot.repository.QuitPlanRepository;
import com.smartquit.smartquitiot.repository.SystemPhaseConditionRepository;
import com.smartquit.smartquitiot.service.*;
import com.smartquit.smartquitiot.toolcalling.QuitPlanTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
    private final PhaseDetailService  phaseDetailService;
    private final PhaseDetailMissionService phaseDetailMissionService;
    private final NotificationService notificationService;
    private final QuitPlanMapper  quitPlanMapper;
    //nho lam cai schedule update status of PHASE
    @Override
    public PhaseDTO getCurrentPhaseAtHomePage() {
        Account account = accountService.getAuthenticatedAccount();
        QuitPlan plan = quitPlanRepository.findByMember_IdAndIsActiveTrue(account.getMember().getId());
        if (plan == null) {
            throw new RuntimeException("Mission Plan Not Found at getCurrentPhaseAtHomePage tools");
        }

        LocalDate currentDate = LocalDate.now();
        Phase currentPhase = phaseRepository.findByStatusAndQuitPlan_Id(PhaseStatus.IN_PROGRESS,plan.getId())
                .orElseGet(() -> {
                    return phaseRepository.findByStatusAndQuitPlan_Id(PhaseStatus.FAILED,plan.getId()).stream()
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "No current actionable phase found (in-progress or failed-keep)"));
                });
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
        phaseDTO.setStatus(currentPhase.getStatus());
        phaseDTO.setCompletedAt(currentPhase.getCompletedAt());
        phaseDTO.setCreateAt(currentPhase.getCreatedAt());
        phaseDTO.setKeepPhase(currentPhase.isKeepPhase());
        phaseDTO.setAvg_cigarettes(currentPhase.getAvg_cigarettes());
        phaseDTO.setAvg_craving_level(currentPhase.getAvg_craving_level());
        phaseDTO.setFm_cigarettes_total(currentPhase.getFm_cigarettes_total());

        if(currentPhaseDetail != null){
            PhaseDetailResponseDTO  phaseDetailResponseDTO = new PhaseDetailResponseDTO();
            phaseDetailResponseDTO.setId(currentPhaseDetail.getId());
            phaseDetailResponseDTO.setName(currentPhaseDetail.getName());
            phaseDetailResponseDTO.setDate(currentPhaseDetail.getDate());
            phaseDetailResponseDTO.setDayIndex(currentPhaseDetail.getDayIndex());
            phaseDetailResponseDTO.setMissionCompleted(missionCompletedInCurrentPhaseDetail);
            phaseDetailResponseDTO.setTotalMission(currentPhaseDetail.getPhaseDetailMissions().size());
            phaseDTO.setCurrentPhaseDetail(phaseDetailResponseDTO);
        }


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
    public PhaseResponse generatePhases(int smokeAvgPerDay, int numberOfYearsSmoking, LocalDate startDate, int FTND, Account account) {
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
                smokeAvgPerDay,
                numberOfYearsSmoking,
                FTND,
                startDate);
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
    public QuitPlanResponse redoPhaseInFailed(RedoPhaseRequest redoPhaseRequest) {

        Phase oldPhase = phaseRepository.findById(redoPhaseRequest.getPhaseId())
                .orElseThrow(() -> new IllegalArgumentException("Phase not found: " + redoPhaseRequest.getPhaseId()));

        QuitPlan plan = oldPhase.getQuitPlan();
        if (plan == null) {
            throw new IllegalStateException("Phase has no QuitPlan");
        }
        if(oldPhase.getStatus() != PhaseStatus.FAILED){
            throw new IllegalStateException("Phase not failed to redo");
        }
        oldPhase.setRedo(true);
        phaseRepository.save(oldPhase); //set thành old
        LocalDate anchorStart = redoPhaseRequest.getAnchorStart() != null
                ? redoPhaseRequest.getAnchorStart()
                : LocalDate.now();

        Phase freshPhase = new Phase();
        freshPhase.setQuitPlan(plan);
        freshPhase.setName(oldPhase.getName());
        freshPhase.setDurationDays(Math.max(1, oldPhase.getDurationDays()));
        freshPhase.setSystemPhaseCondition(oldPhase.getSystemPhaseCondition());
        freshPhase.setCondition(oldPhase.getCondition());            // JSON điều kiện
        freshPhase.setReason(oldPhase.getReason());
        freshPhase.setTotalMissions(0);
        freshPhase.setCompletedMissions(0);
        freshPhase.setProgress(BigDecimal.ZERO);
        freshPhase.setKeepPhase(false);
        freshPhase.setRedo(false);
        freshPhase.setStatus(anchorStart.equals(LocalDate.now())
                ? PhaseStatus.IN_PROGRESS : PhaseStatus.CREATED);
        freshPhase.setAvg_craving_level(0d);
        freshPhase.setAvg_cigarettes(0d);
        freshPhase.setFm_cigarettes_total(0d);

        LocalDate start = anchorStart;
        LocalDate end   = start.plusDays(freshPhase.getDurationDays() - 1);
        freshPhase.setStartDate(start);
        freshPhase.setEndDate(end);

        // Gắn vào list phases ngay sau phase cũ
        List<Phase> phases = plan.getPhases();
        // sort lại phase
        phases.sort(Comparator.comparing(Phase::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Phase::getId));
        int oldIdx = -1;
        for (int i = 0; i < phases.size(); i++) {
            if (Objects.equals(phases.get(i).getId(), oldPhase.getId())) {
                oldIdx = i;
                break;
            }
        }
        if (oldIdx == -1) throw new IllegalStateException("Old phase is not in plan phases list");

        // Chèn fresh ngay sau old
        phases.add(oldIdx + 1, freshPhase);
        // lưu fresh
        freshPhase = phaseRepository.save(freshPhase);

        // 5) RESCHEDULE các phase phía sau FRESH trước khi generate
        LocalDate nextStart = freshPhase.getEndDate().plusDays(1);
        // completedIndex là chỉ số của fresh sau khi chèn: oldIdx + 1
        List<Phase> updated = rescheduleFollowingPhases(plan, oldIdx + 1, nextStart);

        List<PhaseDetail> preparedDetails = phaseDetailService.generatePhaseDetailsForPhase(freshPhase);
        phaseDetailMissionService.generatePhaseDetailMissionsForPhaseInScheduler(
                freshPhase,
                preparedDetails,
                plan,
                4,
                freshPhase.getName(),
                mapPhaseNameToEnum(freshPhase)
        );
        return quitPlanMapper.toResponse(plan);
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
            if(phase.getStartDate().equals(LocalDate.now())){
                phase.setStatus(PhaseStatus.IN_PROGRESS);
            }else{
                phase.setStatus(PhaseStatus.CREATED);
            }
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

    //update status cua plan va quit plan
    //tam thoi server ko co. nen la de 1p chay 1 lan cho de test
    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    @Override
    public void updateQuitPlanAndPhaseStatuses() {
        log.info("Scheduler running.");
        LocalDate currentDate = LocalDate.now();
        // con truong hop failed
        // Lấy tất cả plan đang chạy trong hệ thống
        List<QuitPlan> activePlans =
                quitPlanRepository.findByStatusIn(List.of(QuitPlanStatus.CREATED, QuitPlanStatus.IN_PROGRESS));

        if (activePlans.isEmpty()) {
//            log.info("Không có QuitPlan CREATED/IN_PROGRESS để cập nhật.");
            return;
        }

        for (QuitPlan currentPlan : activePlans) {
            // Trước ngày bắt đầu thì tất cả CREATED
            if (currentDate.isBefore(currentPlan.getStartDate())) {
                if (currentPlan.getStatus() != QuitPlanStatus.CREATED) currentPlan.setStatus(QuitPlanStatus.CREATED);
                for (Phase p : currentPlan.getPhases()) {
                    if (p.getStatus() != PhaseStatus.CREATED) p.setStatus(PhaseStatus.CREATED);
                    phaseRepository.save(p);
                }
                quitPlanRepository.save(currentPlan);
                log.info("Plan {} giữ trạng thái CREATED (chưa đến ngày bắt đầu)", currentPlan.getId());
                continue;
            }

            // Đến/sau startDate thì plan IN_PROGRESS
            if (currentPlan.getStatus() != QuitPlanStatus.IN_PROGRESS) {
                currentPlan.setStatus(QuitPlanStatus.IN_PROGRESS);
            }

            FormMetric formMetric = currentPlan.getFormMetric();
            //List<Phase> phases = currentPlan.getPhases();
            //sap xep lai truong hop redo
            List<Phase> ordered = currentPlan.getPhases().stream()
                    .filter(p -> !p.isRedo())
                    .sorted(Comparator.comparing(Phase::getStartDate)
                            .thenComparing(Phase::getId))
                    .toList();
            for (Phase p : ordered) {
                log.info("ss {}",p.getId());
            }
            for (int i = 0; i < ordered.size(); i++) {
                Phase phase = ordered.get(i);
                PhaseStatus oldStatus = phase.getStatus();
                if(phase.isRedo()){
                    continue;
                }
                if (oldStatus == PhaseStatus.COMPLETED) {
                    continue;
                }
                if (currentDate.isBefore(phase.getStartDate())) {
                    phase.setStatus(PhaseStatus.CREATED);
                }
                else if (!currentDate.isAfter(phase.getEndDate())) {
                    // Trong khung thời gian phase
                    if (i == 0 || ordered.get(i - 1).getStatus() == PhaseStatus.COMPLETED) {
                        phase.setStatus(PhaseStatus.IN_PROGRESS);
                    }
                }
                else { // currentDate > endDate

                    Account account = currentPlan.getMember() != null ? currentPlan.getMember().getAccount() : null;
                    boolean passed = evaluateCondition(phase.getCondition(), account, phase, formMetric);
                    if(passed){
                        phase.setStatus(PhaseStatus.COMPLETED);
                        phase.setCompletedAt(LocalDateTime.now());
                        phase.setAvg_cigarettes(account.getMember().getMetric().getAvgCigarettesPerDay());
                        phase.setAvg_craving_level(account.getMember().getMetric().getAvgCravingLevel());
                        phase.setFm_cigarettes_total(currentPlan.getFormMetric().getSmokeAvgPerDay());
                        phaseRepository.save(phase);


                        int nextIndex = i + 1;
                        if (nextIndex < ordered.size()) {
                            Phase next = ordered.get(nextIndex);
                            LocalDate anchor = phase.getCompletedAt().toLocalDate();

                            // Nếu completedAt == startDate của phase kế -> giữ lịch & generate bình thường
                            if (next.getStartDate() != null && anchor.isEqual(next.getStartDate())) {
                                maybeGenerateNextPhase(ordered, i, currentPlan);
                            } else {
                                // Lệch nhịp (kể cả pass do keep hay các lý do khác)
                                // -> chỉnh lại LỊCH TOÀN BỘ phần còn lại
                               log.info("tao o day ne");
                                List<Phase> updatedPhases = rescheduleFollowingPhases(currentPlan, i, anchor);
                                maybeGenerateNextPhase(updatedPhases, i, currentPlan);
                            }
                        }

                        // reset keepPhase
                        if (phase.isKeepPhase()) {
                            phase.setKeepPhase(false);
                            phaseRepository.save(phase);
                        }

                        //thongbao

                    }else{
                        phase.setStatus(PhaseStatus.FAILED);
//                        log.info(" not pass due to fail condition of phase");
                        phaseRepository.save(phase);
                    }


                    //thong bao
                }

                if (oldStatus != phase.getStatus()) {
                    phaseRepository.save(phase);
                    log.info("Phase {} đổi trạng thái: {} → {}", phase.getId(), oldStatus, phase.getStatus());

                    if (phase.getStatus() == PhaseStatus.COMPLETED) {
                        notificationService.saveAndSendPhaseNoti(currentPlan.getMember().getAccount(), phase, PhaseStatus.COMPLETED, 0);
                    } else if (phase.getStatus() == PhaseStatus.IN_PROGRESS) {
                        notificationService.saveAndSendPhaseNoti(currentPlan.getMember().getAccount(), phase, PhaseStatus.IN_PROGRESS, 0);
                    } else if (phase.getStatus() == PhaseStatus.FAILED) {
                        notificationService.saveAndSendPhaseNoti(currentPlan.getMember().getAccount(), phase, PhaseStatus.FAILED, 0);
                    }
                }
            }

            // Nếu tất cả phase COMPLETED thì plan COMPLETED
            boolean allCompleted = ordered.stream().allMatch(p -> p.getStatus() == PhaseStatus.COMPLETED);
            if (allCompleted && currentPlan.getStatus() != QuitPlanStatus.COMPLETED) {
                currentPlan.setStatus(QuitPlanStatus.COMPLETED);
                notificationService.saveAndSendQuitPlanNoti(currentPlan.getMember().getAccount(), currentPlan, QuitPlanStatus.COMPLETED);
                log.info("QuitPlan {} đã hoàn thành toàn bộ.", currentPlan.getId());
            }

            quitPlanRepository.save(currentPlan);
        }
    }
    //nho set end date cua quit plan
    @Transactional
    public List<Phase> rescheduleFollowingPhases(QuitPlan plan, int completedIndex, LocalDate anchorStart) {
        List<Phase> phases = plan.getPhases();
        if (completedIndex >= phases.size() - 1) return phases;

        LocalDate nextStart = anchorStart;
        for (int j = completedIndex + 1; j < phases.size(); j++) {
            Phase p = phases.get(j);
            int dur = Math.max(1, p.getDurationDays());
            LocalDate start = nextStart;
            LocalDate end = start.plusDays(dur - 1);
            p.setStartDate(start);
            p.setEndDate(end);
            p.setStatus(PhaseStatus.CREATED);
            phaseRepository.save(p);
            nextStart = end.plusDays(1);
        }
        LocalDate planEnd = phases.stream()
                .map(Phase::getEndDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        plan.setEndDate(planEnd);
        quitPlanRepository.save(plan);
        return phases;
    }



    private void maybeGenerateNextPhase(List<Phase> phases, int currentIndex, QuitPlan plan) {
        int nextIndex = currentIndex + 1;
        if (nextIndex >= phases.size()) return; // không có phase kế tiếp

        Phase next = phases.get(nextIndex);

        List<PhaseDetail> preparedDetails = phaseDetailService.generatePhaseDetailsForPhase(next);

        phaseDetailMissionService.generatePhaseDetailMissionsForPhaseInScheduler(
                next,
                preparedDetails,
                plan,
                4,
                next.getName(),
                mapPhaseNameToEnum(next)
        );

        log.info("Đã generate PhaseDetail & Missions cho phase kế tiếp: {} (ID {}).", next.getName(), next.getId());
    }
    private boolean evaluateCondition(JsonNode node, Account account, Phase phase, FormMetric formMetric) {
        String logic = node.has("logic") ? node.get("logic").asText("AND") : "AND";
        boolean result = logic.equalsIgnoreCase("AND");

        for (JsonNode rule : node.get("rules")) {
            boolean current;

            if (rule.has("rules")) {
                current = evaluateCondition(rule, account, phase, formMetric);
            } else {
                String field = rule.get("field").asText();
                String operator = rule.get("operator").asText();

                Object actualValue = getMetricValueDynamic(field, account, phase, formMetric);
                double expected = 0;

                if (rule.has("formula")) {
                    JsonNode f = rule.get("formula");
                    String base = f.get("base").asText();
                    String fop = f.has("operator") ? f.get("operator").asText("*") : "*";
                    double percent = f.get("percent").asDouble();

                    Object baseVal = getMetricValueDynamic(base, account, phase, formMetric);
                    if (baseVal instanceof Number baseNum) {
                        double baseValue = baseNum.doubleValue();
                        expected = switch (fop) {
                            case "*" -> baseValue * percent;
                            case "+" -> baseValue + percent;
                            case "-" -> baseValue - percent;
                            case "/" -> baseValue / percent;
                            default -> baseValue;
                        };
                    }
                } else if (rule.has("value") && rule.get("value").isNumber()) {
                    expected = rule.get("value").asDouble();
                }

                current = (actualValue instanceof Number actualNum)
                        && compare(actualNum.doubleValue(), Operator.fromSymbol(operator), expected);
            }

            if (logic.equalsIgnoreCase("AND")) result &= current;
            else if (logic.equalsIgnoreCase("OR")) result |= current;
        }
        return result;
    }


    private Object getMetricValueDynamic(String field, Account account, Phase phase,FormMetric formMetric) {
        if (field == null || account == null || account.getMember() == null) return null;

        String key = field.trim().toLowerCase().replace(" ", "_");
        return switch (key) {
            case "progress" -> phase.getProgress();
            case "craving_level_avg" -> account.getMember().getMetric().getAvgCravingLevel();
            case "avg_cigarettes" -> account.getMember().getMetric().getAvgCigarettesPerDay();
            case "fm_cigarettes_total" -> formMetric.getSmokeAvgPerDay();
            case "avg_mood" -> account.getMember().getMetric().getAvgMood();
            case "avg_anxiety" -> account.getMember().getMetric().getAvgAnxiety();
            case "avg_confident" -> account.getMember().getMetric().getAvgConfidentLevel();

            default -> null;
        };
    }


    private boolean compare(double actual, Operator op, double expected) {
        return switch (op) {
            case LT -> actual < expected;
            case LE -> actual <= expected;
            case EQ -> Double.compare(actual, expected) == 0;
            case GE -> actual >= expected;
            case GT -> actual > expected;
        };
    }


    private MissionPhase mapPhaseNameToEnum(Phase phase) {
        if (phase == null || phase.getName() == null) {
            return null;
        }

        switch (phase.getName().trim().toLowerCase()) {
            case "preparation":
                return MissionPhase.PREPARATION;
            case "onset":
                return MissionPhase.ONSET;
            case "peak craving":
                return MissionPhase.PEAK_CRAVING;
            case "subsiding":
                return MissionPhase.SUBSIDING;
            case "maintenance":
                return MissionPhase.MAINTENANCE;
            default:
                throw new IllegalArgumentException("Unknown phase name: " + phase.getName());
        }
    }

}

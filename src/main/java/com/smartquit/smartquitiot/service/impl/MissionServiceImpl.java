package com.smartquit.smartquitiot.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartquit.smartquitiot.dto.request.CreateMissionRequest;
import com.smartquit.smartquitiot.dto.request.UpdateMissionRequest;
import com.smartquit.smartquitiot.dto.response.MissionDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.enums.MissionStatus;
import com.smartquit.smartquitiot.enums.Operator;
import com.smartquit.smartquitiot.mapper.MissionMapper;
import com.smartquit.smartquitiot.repository.InterestCategoryRepository;
import com.smartquit.smartquitiot.repository.MissionRepository;
import com.smartquit.smartquitiot.repository.MissionTypeRepository;
import com.smartquit.smartquitiot.service.MissionService;
import com.smartquit.smartquitiot.specifications.MissionSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissionServiceImpl implements MissionService {
    private final MissionRepository missionRepository;
    private final ObjectMapper objectMapper;
    private final MissionMapper missionMapper;
    private final MissionTypeRepository missionTypeRepository;
    private final InterestCategoryRepository  interestCategoryRepository;
    @Override
    public List<Mission> filterMissionsForPhase(
            QuitPlan plan, Account account,
            MissionPhase missionPhase, MissionStatus missionStatus
    ) {
        FormMetric formMetric = plan.getFormMetric();
        List<String> userInterests = formMetric.getInterests(); // danh sách interest từ form
        List<Mission> all = missionRepository.findByPhaseAndStatus(missionPhase, missionStatus);
        log.info("all mission: {}", all.size());
        return all.stream()
                .filter(m -> {
                    if (userInterests == null || userInterests.isEmpty()) return true;
                    var interestCat = m.getInterestCategory();
                    if (interestCat == null) return true;
                    return userInterests.stream()
                            .anyMatch(i -> i.equalsIgnoreCase(interestCat.getName()));
                })
                .filter(m -> checkRuleSatisfied(m, formMetric, account,plan))
                .toList();
    }



    private boolean checkRuleSatisfied(Mission mission, FormMetric metric, Account account, QuitPlan plan) {
        if (mission.getCondition() == null) return true; // ko role -> pass
        try {
            JsonNode condition = mission.getCondition();
            return evaluateCondition(condition, metric, account,plan);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean evaluateCondition(JsonNode condition, FormMetric metric, Account account, QuitPlan plan) {
        String logic = condition.has("logic") ? condition.get("logic").asText("AND") : "AND";
        JsonNode rules = condition.get("rules");

        boolean result = logic.equalsIgnoreCase("AND");

        for (JsonNode rule : rules) {
            boolean current;

            if (rule.has("rules")) {
                // rule con (nested)
                current = evaluateCondition(rule, metric, account, plan);
            } else {
                String field = rule.get("field").asText();
                String operator = rule.get("operator").asText();


                Object actualValue = getMetricValueDynamic(field, metric, account, plan);
                JsonNode valueNode = rule.get("value");
                log.info("Check rule: field={}, operator={}, expected={}, actual={}",
                        field, operator, valueNode, actualValue);

                // xử lý boolean / string / numeric
                if (valueNode.isBoolean()) {
                    boolean expected = valueNode.asBoolean();
                    current = (actualValue instanceof Boolean && (Boolean) actualValue == expected);
                } else if (valueNode.isTextual()) {
                    String expected = valueNode.asText();
                    current = (actualValue != null && actualValue.toString().equalsIgnoreCase(expected));
                } else {
                    double expected = valueNode.asDouble();
                    if (actualValue instanceof Number actualNum) {
                        current = compare(actualNum.doubleValue(), Operator.fromSymbol(operator), expected);
                    } else current = false;
                }
            }

            if (logic.equalsIgnoreCase("AND")) result &= current;
            else if (logic.equalsIgnoreCase("OR")) result |= current;
        }

        return result;
    }


    private Object getMetricValueDynamic(String field, FormMetric formMetric, Account account, QuitPlan plan) {
        if (field == null || account == null || account.getMember() == null) return null;

        String key = field.trim().toLowerCase().replace(" ", "_");
        return switch (key) {
            case "avg_confident_level" -> account.getMember().getMetric().getAvgConfidentLevel();
            case "avg_craving_level" -> account.getMember().getMetric().getAvgCravingLevel();
            case "avg_mood" -> account.getMember().getMetric().getAvgMood();
            case "avg_anxiety" -> account.getMember().getMetric().getAvgAnxiety();
            case "streaks" -> account.getMember().getMetric().getStreaks();
            case "relapse_count_in_phase" -> account.getMember().getMetric().getRelapseCountInPhase();
            case "use_nrt" -> plan.isUseNRT();  //  boolean field
            case "morning_smoking_frequency" -> formMetric.isMorningSmokingFrequency();
            case "minutes_after_waking_to_smoke" -> formMetric.getMinutesAfterWakingToSmoke();
            case "smoke_avg_per_day" ->  formMetric.getSmokeAvgPerDay();
            case "mt_smoke_avg_per_day" -> account.getMember().getMetric().getAvgCigarettesPerDay();
            case "steps" -> account.getMember().getMetric().getSteps();
            case "heart_rate" -> account.getMember().getMetric().getHeartRate();
            case "spo2" -> account.getMember().getMetric().getSpo2();
            case "sleep_duration" -> account.getMember().getMetric().getSleepDuration();
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

//    @Override
//    public Page<MissionDTO> getAllMissions(int page, int size) {
//        PageRequest pageRequest = PageRequest.of(page, size);
//        Page<Mission> missions = missionRepository.findAll(pageRequest);
//        return missions.map(missionMapper::toMissionDTO);
//    }
    @Override
    public Page<MissionDTO> getAllMissions(int page, int size, String search, String status,String phase) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());

        Specification<Mission> spec = MissionSpecification.filterMissions(search, status,phase);

        Page<Mission> missions = missionRepository.findAll(spec, pageable);
        return missions.map(missionMapper::toMissionDTO);
    }
    @Override
    public MissionDTO deleteMission(int id) {
        Mission mission = missionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("mission not found"));
        mission.setStatus(MissionStatus.INACTIVE);
        return missionMapper.toMissionDTO(missionRepository.save(mission));
    }

    @Override
    public MissionDTO getDetails(int id) {
        Mission mission = missionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("mission not found"));
        return missionMapper.toMissionDTO(missionRepository.save(mission));
    }

    @Override
    public MissionDTO createMission(CreateMissionRequest request) {
        if (missionRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Mission with code '" + request.getCode() + "' already exists");
        }
        MissionType missionType = missionTypeRepository.findById(request.getMissionTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Mission type not found with id: " + request.getMissionTypeId()));

        InterestCategory interestCategory = null;
        if (request.getInterestCategoryId() != null) {
            interestCategory = interestCategoryRepository.findById(request.getInterestCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Interest category not found with id: " + request.getInterestCategoryId()));
        }
        JsonNode conditionNode = null;
        if(request.getCondition() != null && !request.getCondition().isEmpty()) {

            try {
                conditionNode = objectMapper.readTree(request.getCondition());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid condition JSON format: " + e.getMessage());
            }
        }
        Mission mission = new Mission();
        mission.setCode(request.getCode());
        mission.setName(request.getName());
        mission.setDescription(request.getDescription());
        mission.setPhase(request.getPhase());
        mission.setStatus(request.getStatus());
        mission.setExp(request.getExp());
        mission.setMissionType(missionType);
        mission.setInterestCategory(interestCategory);
        mission.setCondition(conditionNode);
        Mission savedMission = missionRepository.save(mission);
        return missionMapper.toMissionDTO(savedMission);
    }

    @Override
    public MissionDTO updateMission(int id, UpdateMissionRequest request) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found with id: " + id));


        if (!mission.getCode().equals(request.getCode())) {
            if (missionRepository.existsByCode(request.getCode())) {
                throw new IllegalArgumentException("Mission with code '" + request.getCode() + "' already exists");
            }
        }

        MissionType missionType = missionTypeRepository.findById(request.getMissionTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Mission type not found with id: " + request.getMissionTypeId()));

        InterestCategory interestCategory = null;
        if (request.getInterestCategoryId() != null) {
            interestCategory = interestCategoryRepository.findById(request.getInterestCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Interest category not found with id: " + request.getInterestCategoryId()));
        }

        JsonNode conditionNode = null;
        if(request.getCondition() != null && !request.getCondition().isEmpty()) {

            try {
                conditionNode = objectMapper.readTree(request.getCondition());
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid condition JSON format: " + e.getMessage());
            }
        }
        mission.setCode(request.getCode());
        mission.setName(request.getName());
        mission.setDescription(request.getDescription());
        mission.setPhase(request.getPhase());
        mission.setStatus(request.getStatus());
        mission.setExp(request.getExp());
        mission.setMissionType(missionType);
        mission.setInterestCategory(interestCategory);
        mission.setCondition(conditionNode);

        Mission updatedMission = missionRepository.save(mission);

        return missionMapper.toMissionDTO(updatedMission);
    }
}

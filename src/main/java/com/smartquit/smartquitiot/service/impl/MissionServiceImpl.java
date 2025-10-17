package com.smartquit.smartquitiot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.MissionPhase;
import com.smartquit.smartquitiot.enums.MissionStatus;
import com.smartquit.smartquitiot.enums.Operator;
import com.smartquit.smartquitiot.repository.MissionRepository;
import com.smartquit.smartquitiot.service.MissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissionServiceImpl implements MissionService {
    private final MissionRepository missionRepository;
    private final ObjectMapper objectMapper;

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
            case "avg_confident_level" -> account.getMember().getMetric().getAvg_confident_level();
            case "avg_craving_level" -> account.getMember().getMetric().getAvg_craving_level();
            case "avg_mood" -> account.getMember().getMetric().getAvg_mood();
            case "avg_anxiety" -> account.getMember().getMetric().getAvg_anxiety();
            case "streaks" -> account.getMember().getMetric().getStreaks();
            case "relapse_count_in_phase" -> account.getMember().getMetric().getRelapse_count_in_phase();
            case "use_nrt" -> plan.isUseNRT();  //  boolean field
            case "morning_smoking_frequency" -> formMetric.isMorningSmokingFrequency();
            case "minutes_after_waking_to_smoke" -> formMetric.getMinutesAfterWakingToSmoke();
            case "smoke_avg_per_day" ->  formMetric.getSmokeAvgPerDay();

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



}

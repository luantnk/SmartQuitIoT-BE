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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionServiceImpl implements MissionService {
    private final MissionRepository missionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<Mission> filterMissionsForPhaseDetail(PhaseDetail detail, FormMetric metric, Account account) {

        List<Mission> all = missionRepository.findAllByStatus(MissionStatus.ACTIVE);
        if (all.isEmpty()) {
            throw new RuntimeException("findAllByStatus Active is empty");
        }
        MissionPhase phaseEnum = MissionPhase.valueOf(
                detail.getPhase().getName().toUpperCase().replace(" ", "_")
        );

        List<Mission> filtered = all.stream()
                .filter(m -> m.getPhase() == phaseEnum)
                .toList();

        // ko chon so thich cho no la null
        if (metric.getInterests() != null && !metric.getInterests().isEmpty()) {
            filtered = filtered.stream()
                    .filter(m -> m.getInterestCategory() == null ||
                            metric.getInterests().contains(m.getInterestCategory().getName()))
                    .toList();
        }




        filtered = filtered.stream()
                .filter(m -> checkRuleSatisfied(m, metric, account))
                .toList();

        return filtered;

    }



    private boolean checkRuleSatisfied(Mission mission, FormMetric metric, Account account) {
        if (mission.getCondition() == null) return true; // ko role -> pass
        try {
            JsonNode condition = mission.getCondition();
            return evaluateCondition(condition, metric, account);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean evaluateCondition(JsonNode condition, FormMetric metric, Account account) {
        String logic = condition.has("logic") ? condition.get("logic").asText("AND") : "AND";
        JsonNode rules = condition.get("rules");

        boolean result = logic.equalsIgnoreCase("AND");

        for (JsonNode rule : rules) {
            boolean current;

            if (rule.has("rules")) {
                // rule con (nested)
                current = evaluateCondition(rule, metric, account);
            } else {
                String field = rule.get("field").asText();
                String operator = rule.get("operator").asText();
                double value = rule.get("value").asDouble();
                Double actual = getMetricValue(field, metric, account);
                current = (actual != null && compare(actual, Operator.fromSymbol(operator), value));
            }

            if (logic.equalsIgnoreCase("AND")) result &= current;
            else if (logic.equalsIgnoreCase("OR")) result |= current;
        }

        return result;
    }

    private Double getMetricValue(String field, FormMetric formMetric, Account account) {
        if (field == null || account == null || account.getMember() == null) return null;

        String key = field.trim().toLowerCase().replace(" ", "_");

        //    int avg_confident_level;
        //    int avg_craving_level;
        //    int avg_mood;
        //    int avg_anxiety;
        //    int streaks;
        //    int relapse_count_in_phase;

        return switch (key) {
            case "avg_confident_level" -> account.getMember().getMetric().getAvg_confident_level();
            case "avg_craving_level" -> account.getMember().getMetric().getAvg_craving_level();
            case "avg_mood" ->  account.getMember().getMetric().getAvg_mood();
            case "avg_anxiety" -> account.getMember().getMetric().getAvg_anxiety();
            case "streaks" -> (double)account.getMember().getMetric().getStreaks();
            case "relapse_count_in_phase" -> (double)account.getMember().getMetric().getRelapse_count_in_phase();

            default -> null; // field chưa hỗ trợ trong FormMetric
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

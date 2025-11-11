package com.smartquit.smartquitiot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartquit.smartquitiot.dto.request.TestConditionRequest;
import com.smartquit.smartquitiot.dto.request.UpdateSystemPhaseConditionRequest;
import com.smartquit.smartquitiot.dto.response.RuleEvaluationDetail;
import com.smartquit.smartquitiot.dto.response.SystemPhaseConditionDTO;
import com.smartquit.smartquitiot.dto.response.TestConditionResponse;
import com.smartquit.smartquitiot.dto.response.TestData;
import com.smartquit.smartquitiot.entity.SystemPhaseCondition;
import com.smartquit.smartquitiot.enums.Operator;
import com.smartquit.smartquitiot.mapper.SystemPhaseConditionMapper;
import com.smartquit.smartquitiot.repository.SystemPhaseConditionRepository;
import com.smartquit.smartquitiot.service.SystemPhaseConditionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemPhaseConditionServiceImpl implements SystemPhaseConditionService {
    private final SystemPhaseConditionRepository systemPhaseConditionRepository;
    private final SystemPhaseConditionMapper systemPhaseConditionMapper;

    @Override
    public List<SystemPhaseConditionDTO> getAllSystemPhaseCondition() {
        List<SystemPhaseCondition> systemPhaseConditions = systemPhaseConditionRepository.findAll();
        return systemPhaseConditionMapper.toListDTO(systemPhaseConditions);
    }

    @Override
    @Transactional
    public SystemPhaseConditionDTO updateSystemPhaseCondition(Integer id, UpdateSystemPhaseConditionRequest req) {
        SystemPhaseCondition phaseCondition = systemPhaseConditionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("System phase condition not found with id: " + id));

        // Validate condition structure before saving
        validateConditionStructure(req.getCondition());

        phaseCondition.setCondition(req.getCondition());

        SystemPhaseCondition updated = systemPhaseConditionRepository.save(phaseCondition);

        return systemPhaseConditionMapper.toDTO(updated);
    }

    @Override
    public TestConditionResponse testCondition(TestConditionRequest request) {
        JsonNode condition = request.getCondition();
        TestData testData = request.getTestData();

        List<RuleEvaluationDetail> ruleResults = new ArrayList<>();

        //  Sử dụng lại logic evaluateCondition hiện có
        boolean passed = evaluateConditionForTest(condition, testData, ruleResults);

        String details = buildEvaluationDetails(condition, testData, passed, ruleResults);

        return TestConditionResponse.builder()
                .passed(passed)
                .condition(condition)
                .testData(testData)
                .evaluationDetails(details)
                .ruleResults(ruleResults)
                .build();
    }


    private boolean evaluateConditionForTest(JsonNode node, TestData testData, List<RuleEvaluationDetail> ruleResults) {
        String logic = node.has("logic") ? node.get("logic").asText("AND") : "AND";
        boolean result = logic.equalsIgnoreCase("AND");

        for (JsonNode rule : node.get("rules")) {
            boolean current;

            if (rule.has("rules")) {
                // Nested logic group
                current = evaluateConditionForTest(rule, testData, ruleResults);
            } else {
                String field = rule.get("field").asText();
                String operator = rule.get("operator").asText();

                Object actualValue = getMetricValueForTest(field, testData);
                double expected = 0;

                if (rule.has("formula")) {
                    JsonNode f = rule.get("formula");
                    String base = f.get("base").asText();
                    String fop = f.has("operator") ? f.get("operator").asText("*") : "*";
                    double percent = f.get("percent").asDouble();

                    Object baseVal = getMetricValueForTest(base, testData);
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

                //  Track rule evaluation for test response
                ruleResults.add(RuleEvaluationDetail.builder()
                        .field(field)
                        .operator(operator)
                        .expectedValue(expected)
                        .actualValue(actualValue)
                        .passed(current)
                        .description(String.format("%s: %.2f %s %.2f = %s",
                                field,
                                actualValue instanceof Number ? ((Number) actualValue).doubleValue() : 0.0,
                                operator,
                                expected,
                                current ? "✓" : "✗"))
                        .build());
            }

            if (logic.equalsIgnoreCase("AND")) result &= current;
            else if (logic.equalsIgnoreCase("OR")) result |= current;
        }
        return result;
    }

    private Object getMetricValueForTest(String field, TestData testData) {
        if (field == null || testData == null) return null;

        String key = field.trim().toLowerCase().replace(" ", "_");
        return switch (key) {
            case "progress" -> testData.getProgress();
            case "craving_level_avg" -> testData.getAvgCravingLevel();
            case "avg_cigarettes" -> testData.getAvgCigarettesPerDay();
            case "fm_cigarettes_total" -> testData.getFmCigarettesTotal();
            case "avg_mood" -> testData.getAvgMood();
            case "avg_anxiety" -> testData.getAvgAnxiety();
            case "avg_confident" -> testData.getAvgConfidentLevel();
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

    private String buildEvaluationDetails(JsonNode condition, TestData testData, boolean passed, List<RuleEvaluationDetail> results) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Overall Result: %s\n\n", passed ? "✓ PASSED" : "✗ FAILED"));
        sb.append(String.format("Logic: %s\n\n", condition.get("logic").asText("AND")));
        sb.append("Test Data:\n");
        sb.append(String.format("  - progress: %.2f\n", testData.getProgress() != null ? testData.getProgress() : 0.0));
        sb.append(String.format("  - avg_cigarettes: %.2f\n", testData.getAvgCigarettesPerDay() != null ? testData.getAvgCigarettesPerDay() : 0.0));
        sb.append(String.format("  - craving_level_avg: %.2f\n", testData.getAvgCravingLevel() != null ? testData.getAvgCravingLevel() : 0.0));
        sb.append(String.format("  - avg_mood: %.2f\n", testData.getAvgMood() != null ? testData.getAvgMood() : 0.0));
        sb.append(String.format("  - avg_anxiety: %.2f\n", testData.getAvgAnxiety() != null ? testData.getAvgAnxiety() : 0.0));
        sb.append(String.format("  - avg_confident: %.2f\n", testData.getAvgConfidentLevel() != null ? testData.getAvgConfidentLevel() : 0.0));
        if (testData.getFmCigarettesTotal() != null) {
            sb.append(String.format("  - fm_cigarettes_total: %.2f\n", testData.getFmCigarettesTotal()));
        }
        sb.append("\nRule Evaluations:\n");

        for (int i = 0; i < results.size(); i++) {
            RuleEvaluationDetail detail = results.get(i);
            sb.append(String.format("  %d. %s\n", i + 1, detail.getDescription()));
        }

        return sb.toString();
    }







    private void validateConditionStructure(JsonNode condition) {
        if (condition == null || !condition.has("logic") || !condition.has("rules")) {
            throw new IllegalArgumentException("Invalid condition structure: must have 'logic' and 'rules' fields");
        }

        String logic = condition.get("logic").asText();
        if (!logic.equals("AND") && !logic.equals("OR")) {
            throw new IllegalArgumentException("Invalid logic operator: must be 'AND' or 'OR'");
        }

        // Validate rules recursively
        validateRules(condition.get("rules"));
    }

    private void validateRules(JsonNode rules) {
        if (!rules.isArray()) {
            throw new IllegalArgumentException("Rules must be an array");
        }

        for (JsonNode rule : rules) {
            if (rule.has("rules")) {
                // Nested logic group
                if (!rule.has("logic")) {
                    throw new IllegalArgumentException("Nested logic group must have 'logic' field");
                }
                validateRules(rule.get("rules"));
            } else {
                // Individual rule
                if (!rule.has("field") || !rule.has("operator")) {
                    throw new IllegalArgumentException("Rule must have 'field' and 'operator' fields");
                }

                if (!rule.has("value") && !rule.has("formula")) {
                    throw new IllegalArgumentException("Rule must have either 'value' or 'formula' field");
                }

                // Validate formula structure if present
                if (rule.has("formula")) {
                    JsonNode formula = rule.get("formula");
                    if (!formula.has("base") || !formula.has("operator") || !formula.has("percent")) {
                        throw new IllegalArgumentException("Formula must have 'base', 'operator', and 'percent' fields");
                    }
                }
            }
        }
    }
}

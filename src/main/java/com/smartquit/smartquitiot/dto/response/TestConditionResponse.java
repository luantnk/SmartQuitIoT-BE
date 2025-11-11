package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TestConditionResponse {
    private Boolean passed;
    private JsonNode condition;
    private TestData testData;
    private String evaluationDetails;
    private List<RuleEvaluationDetail> ruleResults;
}

package com.smartquit.smartquitiot.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RuleEvaluationDetail {
    private String field;
    private String operator;
    private Object expectedValue;
    private Object actualValue;
    private Boolean passed;
    private String description;
}

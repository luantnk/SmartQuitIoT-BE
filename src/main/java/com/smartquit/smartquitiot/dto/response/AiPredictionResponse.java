package com.smartquit.smartquitiot.dto.response;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AiPredictionResponse {
    @JsonProperty("success_probability")
    private float successProbability;

    @JsonProperty("relapse_risk")
    private float relapseRisk;

    private String recommendation;
}
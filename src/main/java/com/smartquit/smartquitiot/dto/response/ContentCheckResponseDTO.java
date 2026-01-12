package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ContentCheckResponseDTO {
    @JsonProperty("isToxic")
    private boolean isToxic;
    private String type;
    private String message;
}
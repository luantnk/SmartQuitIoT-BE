package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhaseDTO {
    private int id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private int durationDay;
    private String reason;
    int totalMissions;
    int completedMissions;
    BigDecimal progress;
    JsonNode condition;
    List<PhaseDetailResponseDTO> details;
}

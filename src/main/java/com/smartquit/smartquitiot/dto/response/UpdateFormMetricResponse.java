package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateFormMetricResponse {
    FormMetricDTO formMetricDTO;
    boolean alert; // because when updating it may be affect the FTND score of the Quit Plan. So I flagged this field to alert member hihi
    int fntd_score;
}

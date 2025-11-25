package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartquit.smartquitiot.enums.Gender;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
//@JsonInclude(JsonInclude.Include.NON_NULL)
public class MemberListItemDTO {
    int id; // memberId
    String firstName;
    String lastName;
    String avatarUrl;
    Gender gender;
    LocalDate dob;
    Integer age;
    boolean isUsedFreeTrial;
    // metric summary
    Integer streaks;
    Double smokeFreeDayPercentage;
    Double reductionPercentage;
}

package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartquit.smartquitiot.enums.PhaseEnum;
import com.smartquit.smartquitiot.enums.ReminderType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReminderTemplateDTO {

    int id;
    PhaseEnum phaseEnum;
    ReminderType reminderType;
    String content;
    String triggerCode;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

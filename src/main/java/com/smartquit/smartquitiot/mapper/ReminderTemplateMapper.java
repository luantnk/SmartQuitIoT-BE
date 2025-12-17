package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.AchievementDTO;
import com.smartquit.smartquitiot.dto.response.ReminderTemplateDTO;
import com.smartquit.smartquitiot.entity.Achievement;
import com.smartquit.smartquitiot.entity.ReminderTemplate;
import com.smartquit.smartquitiot.enums.PhaseEnum;
import com.smartquit.smartquitiot.enums.ReminderType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ReminderTemplateMapper {
    public ReminderTemplateDTO toReminderTemplateDTO(ReminderTemplate reminderTemplate) {
        if(reminderTemplate == null) {
            return null;
        }
        ReminderTemplateDTO reminderTemplateDTO = new ReminderTemplateDTO();
        reminderTemplateDTO.setId(reminderTemplate.getId());
        reminderTemplateDTO.setPhaseEnum(reminderTemplate.getPhaseEnum());
        reminderTemplateDTO.setReminderType(reminderTemplate.getReminderType());
        reminderTemplateDTO.setContent(reminderTemplate.getContent());
        reminderTemplateDTO.setTriggerCode(reminderTemplate.getTriggerCode());
        reminderTemplateDTO.setCreatedAt(reminderTemplate.getCreatedAt());
        reminderTemplateDTO.setUpdatedAt(reminderTemplate.getUpdatedAt());
        return reminderTemplateDTO;
    }
}

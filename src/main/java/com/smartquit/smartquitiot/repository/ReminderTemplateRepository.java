package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.ReminderTemplate;
import com.smartquit.smartquitiot.enums.PhaseEnum;
import com.smartquit.smartquitiot.enums.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReminderTemplateRepository extends JpaRepository<ReminderTemplate,Integer> {
    List<ReminderTemplate> findByReminderTypeAndPhaseEnum(ReminderType reminderType, PhaseEnum phaseEnum);

    List<ReminderTemplate> findByReminderType(ReminderType reminderType);
}

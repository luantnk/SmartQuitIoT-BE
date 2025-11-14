package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.entity.Phase;
import com.smartquit.smartquitiot.entity.PhaseDetail;

import java.util.List;

public interface ReminderQueueService {
 void  createDailyRemindersForPhase(Phase phase, List<PhaseDetail> details);
}

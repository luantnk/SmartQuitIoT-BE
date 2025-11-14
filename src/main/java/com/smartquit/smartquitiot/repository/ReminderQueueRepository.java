package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.ReminderQueue;
import com.smartquit.smartquitiot.enums.ReminderQueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReminderQueueRepository extends JpaRepository<ReminderQueue,Integer> {
    List<ReminderQueue> findByStatusAndScheduledAtBefore(ReminderQueueStatus reminderQueueStatus, LocalDateTime now);
}

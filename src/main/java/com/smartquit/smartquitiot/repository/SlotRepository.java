package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<Slot, Integer> {
    Optional<Slot> findByStartTimeAndEndTime(LocalTime startTime, LocalTime endTime);
}

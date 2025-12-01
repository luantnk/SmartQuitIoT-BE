package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Slot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<Slot, Integer> {
    Optional<Slot> findByStartTimeAndEndTime(LocalTime startTime, LocalTime endTime);
    
    // Find all slots ordered by startTime ascending
    List<Slot> findAllByOrderByStartTimeAsc();
    
    // Find all slots with pagination, ordered by startTime ascending
    Page<Slot> findAllByOrderByStartTimeAsc(Pageable pageable);
}

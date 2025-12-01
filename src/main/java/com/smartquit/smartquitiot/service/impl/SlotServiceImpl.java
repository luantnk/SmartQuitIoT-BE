package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.SlotDTO;
import com.smartquit.smartquitiot.dto.response.SlotReseedResponse;
import com.smartquit.smartquitiot.entity.Appointment;
import com.smartquit.smartquitiot.entity.CoachWorkSchedule;
import com.smartquit.smartquitiot.entity.Feedback;
import com.smartquit.smartquitiot.entity.Slot;
import com.smartquit.smartquitiot.mapper.SlotMapper;
import com.smartquit.smartquitiot.repository.AppointmentRepository;
import com.smartquit.smartquitiot.repository.CoachWorkScheduleRepository;
import com.smartquit.smartquitiot.repository.FeedbackRepository;
import com.smartquit.smartquitiot.repository.SlotRepository;
import com.smartquit.smartquitiot.service.SlotService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotServiceImpl implements SlotService {

    private final SlotRepository slotRepository;
    private final SlotMapper slotMapper;
    private final AppointmentRepository appointmentRepository;
    private final CoachWorkScheduleRepository coachWorkScheduleRepository;
    private final FeedbackRepository feedbackRepository;

    @Override
    @Transactional
    public List<Slot> findOrCreateRange(LocalTime start, LocalTime end, int slotMinutes, int gapMinutes) {
        if (slotMinutes <= 0) throw new IllegalArgumentException("slotMinutes must be > 0");
        if (gapMinutes < 0) throw new IllegalArgumentException("gapMinutes must be >= 0");
        List<Slot> out = new ArrayList<>();
        LocalTime cur = start;

        while (true) {
            LocalTime next = cur.plusMinutes(slotMinutes);
            if (next.isAfter(end)) break;

            // tìm tồn tại, nếu không có -> tạo (catch race)
            Optional<Slot> exist = slotRepository.findByStartTimeAndEndTime(cur, next);
            if (exist.isPresent()) {
                out.add(exist.get());
            } else {
                Slot s = new Slot();
                s.setStartTime(cur);
                s.setEndTime(next);
                try {
                    s = slotRepository.save(s);
                    out.add(s);
                } catch (DataIntegrityViolationException dive) {
                    // Race: another thread created it -> fetch again
                    Slot fetched = slotRepository.findByStartTimeAndEndTime(cur, next)
                            .orElseThrow(() -> new IllegalStateException("Failed to create or fetch slot"));
                    out.add(fetched);
                }
            }

            // Slot tiếp theo bắt đầu sau gap
            cur = next.plusMinutes(gapMinutes);
        }

        return out;
    }

    @Override
    public List<Slot> listAll() {
        return slotRepository.findAllByOrderByStartTimeAsc();
    }

    @Override
    public Page<SlotDTO> listAllSlots(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Slot> slots = slotRepository.findAllByOrderByStartTimeAsc(pageRequest);
        return slots.map(slotMapper::toSlotDTO);
    }

    @Override
    @Transactional
    public SlotReseedResponse reseedSlots(LocalTime start, LocalTime end, int slotMinutes, int gapMinutes) {
        // Validate input
        if (slotMinutes <= 0) throw new IllegalArgumentException("slotMinutes must be > 0");
        if (gapMinutes < 0) throw new IllegalArgumentException("gapMinutes must be >= 0");
        if (start.isAfter(end) || start.equals(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        // Check active appointments from today onwards
        LocalDate today = LocalDate.now();
        long activeAppointments = appointmentRepository.countActiveAppointmentsFromDate(today);
        
        if (activeAppointments > 0) {
            throw new IllegalStateException(
                String.format("Cannot reseed slots: %d active appointment(s) exist from today onwards", activeAppointments)
            );
        }

        log.info("Starting slot reseed: start={}, end={}, slotMinutes={}, gapMinutes={}", 
                start, end, slotMinutes, gapMinutes);

        // Get count of slots before reseed
        long slotsBefore = slotRepository.count();
        Set<Integer> slotIdsBefore = slotRepository.findAll().stream()
                .map(Slot::getId)
                .collect(Collectors.toSet());

        // Create new slots (findOrCreateRange will create if not exists, or return existing)
        List<Slot> slotsAfterReseed = findOrCreateRange(start, end, slotMinutes, gapMinutes);
        
        // Count actually new slots (not existed before)
        int createdCount = (int) slotsAfterReseed.stream()
                .filter(slot -> !slotIdsBefore.contains(slot.getId()))
                .count();

        // Get IDs of slots that should exist after reseed (the new config slots)
        Set<Integer> expectedSlotIds = slotsAfterReseed.stream()
                .map(Slot::getId)
                .collect(Collectors.toSet());

        // Cleanup: delete slots that don't match the new config
        // This includes both orphan slots AND old config slots (even if used by schedules)
        int deletedCount = deleteSlotsNotInConfig(expectedSlotIds);

        // Get final count
        long slotsAfter = slotRepository.count();
        int totalSlots = (int) slotsAfter;

        log.info("Slot reseed completed: created={}, deleted={}, total={}", 
                createdCount, deletedCount, totalSlots);

        return SlotReseedResponse.builder()
                .createdCount(createdCount)
                .deletedCount(deletedCount)
                .totalSlots(totalSlots)
                .build();
    }

    /**
     * Delete slots that are not in the expected config (new slots after reseed)
     * This will delete old config slots even if they are used by schedules,
     * since we've already checked that there are no active appointments.
     * 
     * @param expectedSlotIds Set of slot IDs that should exist after reseed
     * @return number of deleted slots
     */
    private int deleteSlotsNotInConfig(Set<Integer> expectedSlotIds) {
        // Get all slots
        List<Slot> allSlots = slotRepository.findAll();
        
        if (allSlots.isEmpty()) {
            return 0;
        }

        // Find slots that are NOT in the expected config (old slots to delete)
        List<Slot> slotsToDelete = allSlots.stream()
                .filter(slot -> !expectedSlotIds.contains(slot.getId()))
                .collect(Collectors.toList());

        if (slotsToDelete.isEmpty()) {
            log.debug("No old slots to delete");
            return 0;
        }

        // Delete old slots (we've already verified no active appointments exist)
        // Need to delete in order: Appointments -> CoachWorkSchedule -> Slots
        try {
            List<Integer> slotIdsToDelete = slotsToDelete.stream()
                    .map(Slot::getId)
                    .collect(Collectors.toList());
            
            // Find CoachWorkSchedule records for these slots
            List<CoachWorkSchedule> schedulesToDelete = 
                    coachWorkScheduleRepository.findAll().stream()
                            .filter(cws -> cws.getSlot() != null && slotIdsToDelete.contains(cws.getSlot().getId()))
                            .collect(Collectors.toList());
            
            if (!schedulesToDelete.isEmpty()) {
                // Step 1: Find all appointments that reference these CoachWorkSchedules
                List<Integer> cwsIdsToDelete = schedulesToDelete.stream()
                        .map(CoachWorkSchedule::getId)
                        .collect(Collectors.toList());
                
                List<Appointment> appointmentsToDelete = 
                        appointmentRepository.findByCoachWorkScheduleIds(cwsIdsToDelete);
                
                if (!appointmentsToDelete.isEmpty()) {
                    // Step 1a: Delete all feedbacks that reference these appointments
                    List<Integer> appointmentIdsToDelete = appointmentsToDelete.stream()
                            .map(Appointment::getId)
                            .collect(Collectors.toList());
                    
                    List<Feedback> feedbacksToDelete = 
                            feedbackRepository.findByAppointmentIds(appointmentIdsToDelete);
                    
                    if (!feedbacksToDelete.isEmpty()) {
                        feedbackRepository.deleteAll(feedbacksToDelete);
                        log.info("Deleted {} feedback(s) for old appointments", feedbacksToDelete.size());
                    }
                    
                    // Step 1b: Delete appointments
                    appointmentRepository.deleteAll(appointmentsToDelete);
                    log.info("Deleted {} appointment(s) for old slots", appointmentsToDelete.size());
                }
                
                // Step 2: Delete CoachWorkSchedule records
                coachWorkScheduleRepository.deleteAll(schedulesToDelete);
                log.info("Deleted {} CoachWorkSchedule record(s) for old slots", schedulesToDelete.size());
            }
            
            // Step 3: Delete the slots
            slotRepository.deleteAll(slotsToDelete);
            log.info("Deleted {} old slot(s) that don't match new config", slotsToDelete.size());
            
            return slotsToDelete.size();
        } catch (Exception e) {
            log.error("Error deleting old slots", e);
            throw new IllegalStateException("Failed to delete old slots: " + e.getMessage(), e);
        }
    }
}

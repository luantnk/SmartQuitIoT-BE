package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.SlotDTO;
import com.smartquit.smartquitiot.dto.response.SlotReseedResponse;
import com.smartquit.smartquitiot.entity.Slot;
import com.smartquit.smartquitiot.mapper.SlotMapper;
import com.smartquit.smartquitiot.repository.AppointmentRepository;
import com.smartquit.smartquitiot.repository.CoachWorkScheduleRepository;
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
        return slotRepository.findAll();
    }

    @Override
    public Page<SlotDTO> listAllSlots(int page, int size) {
        PageRequest  pageRequest = PageRequest.of(page, size);
        Page<Slot> slots = slotRepository.findAll(pageRequest);
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

        // Cleanup orphan slots (slots not used by any CoachWorkSchedule)
        int deletedCount = deleteUnusedSlots();

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
     * Delete slots that are not referenced by any CoachWorkSchedule
     * @return number of deleted slots
     */
    private int deleteUnusedSlots() {
        // Get all slots
        List<Slot> allSlots = slotRepository.findAll();
        
        if (allSlots.isEmpty()) {
            return 0;
        }

        // Get all slot IDs that are being used by CoachWorkSchedule
        Set<Integer> usedSlotIds = coachWorkScheduleRepository.findAll()
                .stream()
                .filter(cws -> cws.getSlot() != null)
                .map(cws -> cws.getSlot().getId())
                .collect(Collectors.toSet());

        // Find orphan slots (not used by any CoachWorkSchedule)
        List<Slot> orphanSlots = allSlots.stream()
                .filter(slot -> !usedSlotIds.contains(slot.getId()))
                .collect(Collectors.toList());

        if (orphanSlots.isEmpty()) {
            log.debug("No orphan slots to delete");
            return 0;
        }

        // Delete orphan slots
        slotRepository.deleteAll(orphanSlots);
        log.info("Deleted {} orphan slot(s)", orphanSlots.size());
        
        return orphanSlots.size();
    }
}

package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.ScheduleAssignRequest;
import com.smartquit.smartquitiot.dto.request.ScheduleUpdateRequest;
import com.smartquit.smartquitiot.dto.response.CoachSummaryDTO;
import com.smartquit.smartquitiot.dto.response.ScheduleByDayResponse;
import com.smartquit.smartquitiot.dto.response.SlotAvailableResponse;
import com.smartquit.smartquitiot.entity.Coach;
import com.smartquit.smartquitiot.entity.CoachWorkSchedule;
import com.smartquit.smartquitiot.entity.Slot;
import com.smartquit.smartquitiot.enums.CoachWorkScheduleStatus;
import com.smartquit.smartquitiot.mapper.CoachMapper;
import com.smartquit.smartquitiot.repository.CoachRepository;
import com.smartquit.smartquitiot.repository.CoachWorkScheduleRepository;
import com.smartquit.smartquitiot.service.ScheduleService;
import com.smartquit.smartquitiot.service.SlotService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {

    private final CoachRepository coachRepository;
    private final CoachWorkScheduleRepository coachWorkScheduleRepository;
    private final SlotService slotService;
    private final CoachMapper coachMapper;

    @Override
    @Transactional
    public int assignCoachesToDates(ScheduleAssignRequest request) {
        if (request.getDates() == null || request.getDates().isEmpty()) {
            throw new IllegalArgumentException("Date list is null");
        }
        if (request.getCoachIds() == null || request.getCoachIds().isEmpty()) {
            throw new IllegalArgumentException("Coach list is null");
        }

        // === Dedupe inputs: prevent duplicate inserts if client sends duplicate dates/coachIds ===
        // Preserve order using LinkedHashSet
        Set<LocalDate> parsedDates = request.getDates().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Integer> coachIdSet = request.getCoachIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (parsedDates.isEmpty() || coachIdSet.isEmpty()) {
            throw new IllegalArgumentException("Dates or coachIds are empty after deduplication");
        }

        // Retrieve pre-seeded slots
        List<Slot> slots = slotService.listAll();
        if (slots.isEmpty()) {
            throw new IllegalStateException("Slot list does not exist in the system");
        }

        List<CoachWorkSchedule> newSchedules = new ArrayList<>();

        // Iterate through each date and coach
        for (LocalDate date : parsedDates) {
            for (Integer coachId : coachIdSet) {
                Optional<Coach> coachOpt = coachRepository.findById(coachId);
                if (coachOpt.isEmpty()) {
                    log.warn("Coach id {} does not exist, skipping", coachId);
                    continue;
                }
                Coach coach = coachOpt.get();

                // Fetch existing slot IDs for this coach and date in one query (minimize DB calls)
                List<CoachWorkSchedule> existing =
                        coachWorkScheduleRepository.findAllByCoachAndDate(coachId, date);
                Set<Integer> existingSlotIds = existing.stream()
                        .map(c -> c.getSlot().getId())
                        .collect(Collectors.toSet());

                for (Slot slot : slots) {
                    if (!existingSlotIds.contains(slot.getId())) {
                        CoachWorkSchedule schedule = new CoachWorkSchedule();
                        schedule.setCoach(coach);
                        schedule.setDate(date);
                        schedule.setSlot(slot);
                        schedule.setStatus(CoachWorkScheduleStatus.AVAILABLE);
                        newSchedules.add(schedule);
                    }
                }
            }
        }

        if (!newSchedules.isEmpty()) {
            try {
                coachWorkScheduleRepository.saveAll(newSchedules);
                log.info("Created {} new work schedules", newSchedules.size());
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                // Race condition: if two requests insert at the same time,
                // a DB unique constraint may throw an exception
                // Catch it here so the service does not crash
                log.warn(
                        "DataIntegrityViolationException when saving schedules (likely duplicates due to concurrent inserts): {}",
                        ex.getMessage()
                );
                // Optionally: re-query actual records or return partial information
            }
        }

        return newSchedules.size();
    }

    @Override
    public List<ScheduleByDayResponse> getSchedulesByMonth(int year, int month) {
        LocalDate today = LocalDate.now();
        YearMonth requested = YearMonth.of(year, month);
        YearMonth current = YearMonth.from(today);

        // Do not allow viewing past months
//        if (requested.isBefore(current)) {
//            log.info("Requested month {}-{} is in the past — returning empty result", year, month);
//            return List.of();
//        }

        LocalDate start = requested.equals(current)
                ? today
                : requested.atDay(1);
        LocalDate end = requested.atEndOfMonth();

        List<CoachWorkSchedule> schedules =
                coachWorkScheduleRepository.findAllByDateBetweenWithCoach(start, end);

        // Group by date
        Map<LocalDate, List<CoachWorkSchedule>> grouped = schedules.stream()
                .collect(Collectors.groupingBy(CoachWorkSchedule::getDate));

        List<ScheduleByDayResponse> result = new ArrayList<>();

        grouped.forEach((date, list) -> {
            List<CoachSummaryDTO> coachList = list.stream()
                    .map(CoachWorkSchedule::getCoach)
                    .distinct()
                    .map(coachMapper::toCoachSummaryDTO)
                    .toList();

            if (!coachList.isEmpty()) {
                result.add(ScheduleByDayResponse.builder()
                        .date(date)
                        .coaches(coachList)
                        .build());
            }
        });

        // Sort ascending by date
        result.sort(Comparator.comparing(ScheduleByDayResponse::getDate));
        return result;
    }

    @Override
    @Transactional
    public void updateScheduleByDate(LocalDate date, ScheduleUpdateRequest request) {
        LocalDate today = LocalDate.now();

        if (date.isBefore(today)) {
            throw new IllegalArgumentException("Cannot modify schedules in the past.");
        }

        List<Slot> slots = slotService.listAll();
        if (slots.isEmpty()) {
            throw new IllegalStateException("Slot list has not been initialized.");
        }

        // Remove coach (only if all slots are AVAILABLE)
        for (Integer coachId : request.getRemoveCoachIds()) {
            List<CoachWorkSchedule> schedules =
                    coachWorkScheduleRepository.findAllByCoachAndDate(coachId, date);
            if (schedules.isEmpty()) continue;

            boolean allAvailable = schedules.stream()
                    .allMatch(s -> s.getStatus() == CoachWorkScheduleStatus.AVAILABLE);

            if (!allAvailable) {
                throw new IllegalStateException(
                        String.format(
                                "Cannot remove coach %d from date %s because some slots are not available (may be booked or in progress).",
                                coachId, date
                        )
                );
            }

            coachWorkScheduleRepository.deleteAll(schedules);
            log.info("Removed work schedules of coach {} on date {}", coachId, date);
        }

        // Add new coach (if not already assigned)
        for (Integer coachId : request.getAddCoachIds()) {
            Optional<Coach> coachOpt = coachRepository.findById(coachId);
            if (coachOpt.isEmpty()) {
                log.warn("Coach id {} does not exist, skipping", coachId);
                continue;
            }
            Coach coach = coachOpt.get();

            List<Integer> existingCoachIds =
                    coachWorkScheduleRepository.findAllCoachIdsByDate(date);
            if (existingCoachIds.contains(coachId)) {
                log.info("Coach {} already has a schedule on date {}, skipping", coachId, date);
                continue;
            }

            List<CoachWorkSchedule> newSchedules = new ArrayList<>();
            for (Slot slot : slots) {
                CoachWorkSchedule schedule = new CoachWorkSchedule();
                schedule.setCoach(coach);
                schedule.setDate(date);
                schedule.setSlot(slot);
                schedule.setStatus(CoachWorkScheduleStatus.AVAILABLE);
                newSchedules.add(schedule);
            }
            coachWorkScheduleRepository.saveAll(newSchedules);
            log.info("Added coach {} to schedule on date {}", coachId, date);
        }
    }

    @Override
    public List<SlotAvailableResponse> getAvailableSlots(int coachId, LocalDate date) {
        if (!coachRepository.existsById(coachId)) {
            throw new IllegalArgumentException("Coach does not exist.");
        }

        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new IllegalArgumentException("Date must not be earlier than today.");
        }

        LocalDateTime now = LocalDateTime.now();

        return coachWorkScheduleRepository
                .findAllByCoachIdAndDateAndStatusWithSlot(
                        coachId, date, CoachWorkScheduleStatus.AVAILABLE
                )
                .stream()
                .filter(cws -> {
                    if (date.isEqual(today)) {
                        LocalDateTime slotStart =
                                LocalDateTime.of(date, cws.getSlot().getStartTime());
                        return !slotStart.isBefore(now);
                    }
                    return true;
                })
                .map(cws -> new SlotAvailableResponse(
                        cws.getSlot().getId(),
                        cws.getSlot().getStartTime(),
                        cws.getSlot().getEndTime()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<LocalDate> getWorkdaysByMonth(int accountId, int year, int month) {
        // Resolve coach by accountId (FE sends accountId)
        var coachOpt = coachRepository.findByAccountId(accountId);
        if (coachOpt.isEmpty()) {
            throw new IllegalArgumentException("Account does not exist");
        }
        int coachId = coachOpt.get().getId();

        YearMonth requested = YearMonth.of(year, month);
        LocalDate start = requested.atDay(1);      // Start from the first day of the month (including past)
        LocalDate end = requested.atEndOfMonth();  // End of the month

        // Retrieve all CoachWorkSchedules in range and filter by coachId
        List<CoachWorkSchedule> schedules =
                coachWorkScheduleRepository.findAllByDateBetweenWithCoach(start, end);

        return schedules.stream()
                .filter(Objects::nonNull)
                .filter(cws -> cws.getCoach() != null && cws.getCoach().getId() == coachId)
                .map(CoachWorkSchedule::getDate)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<CoachSummaryDTO> findAvailableCoaches(
            LocalDate date, int slotId, Integer excludeCoachId
    ) {
        if (date == null) throw new IllegalArgumentException("Date is required");
        if (slotId <= 0) throw new IllegalArgumentException("Invalid slotId");

        // Query all CoachWorkSchedules for this date and slot (including coach join)
        List<CoachWorkSchedule> cwsList =
                coachWorkScheduleRepository.findAllByDateAndSlotIdWithCoach(date, slotId);

        return cwsList.stream()
                .filter(Objects::nonNull)
                .filter(cws -> cws.getStatus() == CoachWorkScheduleStatus.AVAILABLE)
                .filter(cws -> {
                    Coach coach = cws.getCoach();
                    return coach != null && coach.getAccount() != null;
                })
                .filter(cws ->
                        excludeCoachId == null || cws.getCoach().getId() != excludeCoachId
                )
                .map(CoachWorkSchedule::getCoach)
                .distinct()
                .map(coachMapper::toCoachSummaryDTO)
                .toList();
    }
}

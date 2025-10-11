package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.ScheduleAssignRequest;
import com.smartquit.smartquitiot.entity.Coach;
import com.smartquit.smartquitiot.entity.CoachWorkSchedule;
import com.smartquit.smartquitiot.entity.Slot;
import com.smartquit.smartquitiot.repository.CoachRepository;
import com.smartquit.smartquitiot.repository.CoachWorkScheduleRepository;
import com.smartquit.smartquitiot.service.ScheduleService;
import com.smartquit.smartquitiot.service.SlotService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {

    private final CoachRepository coachRepository;
    private final CoachWorkScheduleRepository coachWorkScheduleRepository;
    private final SlotService slotService;
    @Override
    @Transactional
    public int assignCoachesToDates(ScheduleAssignRequest request) {
        if (request.getDates() == null || request.getDates().isEmpty()) {
            throw new IllegalArgumentException("List dates null");
        }
        if (request.getCoachIds() == null || request.getCoachIds().isEmpty()) {
            throw new IllegalArgumentException("List coach null");
        }

        List<LocalDate> parsedDates = new ArrayList<>();
        for (String dateStr : request.getDates()) {
            parsedDates.add(LocalDate.parse(dateStr));
        }

        // lấy danh sách slot đã được seed sẵn (07:00 - 15:00, 30p)
        List<Slot> slots = slotService.listAll();
        if (slots.isEmpty()) {
            throw new IllegalStateException("Slot list is not exist in the system");
        }

        List<CoachWorkSchedule> newSchedules = new ArrayList<>();

        // duyệt từng ngày và từng coach
        for (LocalDate date : parsedDates) {
            for (Integer coachId : request.getCoachIds()) {
                Optional<Coach> coachOpt = coachRepository.findById(coachId);
                if (coachOpt.isEmpty()) {
                    log.warn("Coach id {} không tồn tại, bỏ qua", coachId);
                    continue;
                }
                Coach coach = coachOpt.get();

                for (Slot slot : slots) {
                    boolean exists = coachWorkScheduleRepository
                            .existsByCoachIdAndDateAndSlotId(coachId, date, slot.getId());

                    if (!exists) {
                        CoachWorkSchedule schedule = new CoachWorkSchedule();
                        schedule.setCoach(coach);
                        schedule.setDate(date);
                        schedule.setSlot(slot);
                        newSchedules.add(schedule);
                    }
                }
            }
        }

        if (!newSchedules.isEmpty()) {
            coachWorkScheduleRepository.saveAll(newSchedules);
            log.info("Đã tạo {} lịch làm việc mới", newSchedules.size());
        }

        return newSchedules.size();
    }
}

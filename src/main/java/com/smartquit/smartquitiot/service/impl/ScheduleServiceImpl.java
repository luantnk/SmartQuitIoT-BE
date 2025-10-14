package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.ScheduleAssignRequest;
import com.smartquit.smartquitiot.dto.request.ScheduleUpdateRequest;
import com.smartquit.smartquitiot.dto.response.CoachSummaryDTO;
import com.smartquit.smartquitiot.dto.response.ScheduleByDayResponse;
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
            throw new IllegalArgumentException("List dates null");
        }
        if (request.getCoachIds() == null || request.getCoachIds().isEmpty()) {
            throw new IllegalArgumentException("List coach null");
        }

        List<LocalDate> parsedDates = new ArrayList<>();
        for (LocalDate dateStr : request.getDates()) {
            parsedDates.add(dateStr);
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
                        schedule.setStatus(CoachWorkScheduleStatus.AVAILABLE);
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

    @Override
    public List<ScheduleByDayResponse> getSchedulesByMonth(int year, int month) {
        LocalDate today = LocalDate.now();
        YearMonth requested = YearMonth.of(year, month);
        YearMonth current = YearMonth.from(today);

        // Không cho phép xem tháng quá khứ
        if (requested.isBefore(current)) {
            log.info("Requested month {}-{} is in the past — return empty", year, month);
            return List.of();
        }

        LocalDate start = requested.equals(current)
                ? today
                : requested.atDay(1);
        LocalDate end = requested.atEndOfMonth();

        List<CoachWorkSchedule> schedules =
                coachWorkScheduleRepository.findAllByDateBetweenWithCoach(start, end);

        // Group theo ngày
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

        // sort ascending theo ngày
        result.sort(Comparator.comparing(ScheduleByDayResponse::getDate));
        return result;
    }

    @Override
    @Transactional
    public void updateScheduleByDate(LocalDate date, ScheduleUpdateRequest request) {
        LocalDate today = LocalDate.now();

        if (date.isBefore(today)) {
            throw new IllegalArgumentException("Không thể chỉnh sửa lịch trong quá khứ.");
        }

        List<Slot> slots = slotService.listAll();
        if (slots.isEmpty()) {
            throw new IllegalStateException("Danh sách slot chưa được khởi tạo.");
        }

        // Xóa coach (chỉ khi tất cả slot đều AVAILABLE)
        for (Integer coachId : request.getRemoveCoachIds()) {
            List<CoachWorkSchedule> schedules = coachWorkScheduleRepository.findAllByCoachAndDate(coachId, date);
            if (schedules.isEmpty()) continue;

            boolean allAvailable = schedules.stream()
                    .allMatch(s -> s.getStatus() == CoachWorkScheduleStatus.AVAILABLE);
            if (!allAvailable) {
                log.warn("Coach id {} không thể xóa vì có slot không ở trạng thái AVAILABLE", coachId);
                continue;
            }

            coachWorkScheduleRepository.deleteAll(schedules);
            log.info("Đã xóa lịch làm việc của coach {} trong ngày {}", coachId, date);
        }

        // Thêm coach mới (nếu chưa có)
        for (Integer coachId : request.getAddCoachIds()) {
            Optional<Coach> coachOpt = coachRepository.findById(coachId);
            if (coachOpt.isEmpty()) {
                log.warn("Coach id {} không tồn tại, bỏ qua", coachId);
                continue;
            }
            Coach coach = coachOpt.get();

            List<Integer> existingCoachIds = coachWorkScheduleRepository.findAllCoachIdsByDate(date);
            if (existingCoachIds.contains(coachId)) {
                log.info("Coach {} đã có lịch ngày {}, bỏ qua", coachId, date);
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
            log.info("Đã thêm coach {} vào lịch ngày {}", coachId, date);
        }
    }


}

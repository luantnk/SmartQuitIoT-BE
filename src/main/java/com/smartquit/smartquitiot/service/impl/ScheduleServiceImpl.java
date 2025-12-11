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
            throw new IllegalArgumentException("List dates null");
        }
        if (request.getCoachIds() == null || request.getCoachIds().isEmpty()) {
            throw new IllegalArgumentException("List coach null");
        }

        // === Dedupe inputs: tránh insert trùng nếu client gửi duplicate dates/coachIds ===
        // preserve order: LinkedHashSet
        Set<LocalDate> parsedDates = request.getDates().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Integer> coachIdSet = request.getCoachIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (parsedDates.isEmpty() || coachIdSet.isEmpty()) {
            throw new IllegalArgumentException("Dates or coachIds are empty after dedupe");
        }

        // lấy danh sách slot đã seed sẵn
        List<Slot> slots = slotService.listAll();
        if (slots.isEmpty()) {
            throw new IllegalStateException("Slot list is not exist in the system");
        }

        List<CoachWorkSchedule> newSchedules = new ArrayList<>();

        // duyệt từng ngày và từng coach
        for (LocalDate date : parsedDates) {
            for (Integer coachId : coachIdSet) {
                Optional<Coach> coachOpt = coachRepository.findById(coachId);
                if (coachOpt.isEmpty()) {
                    log.warn("Coach id {} không tồn tại, bỏ qua", coachId);
                    continue;
                }
                Coach coach = coachOpt.get();

                // fetch existing slot ids for this coach & date in one query (minimize DB calls)
                List<CoachWorkSchedule> existing = coachWorkScheduleRepository.findAllByCoachAndDate(coachId, date);
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
                log.info("Đã tạo {} lịch làm việc mới", newSchedules.size());
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                // Race condition: nếu 2 request insert cùng lúc, DB unique constraint sẽ ném lỗi
                // Bắt lại ở đây để service không crash; log để dev biết
                log.warn("DataIntegrityViolationException when saving schedules (likely duplicate due to concurrent insert): {}",
                        ex.getMessage());
                // Optionally: re-query how many actual records now exist or return partial info.
            }
        }

        return newSchedules.size();
    }


    @Override
    public List<ScheduleByDayResponse> getSchedulesByMonth(int year, int month) {
        LocalDate today = LocalDate.now();
        YearMonth requested = YearMonth.of(year, month);
        YearMonth current = YearMonth.from(today);

        // Không cho phép xem tháng quá khứ
//        if (requested.isBefore(current)) {
//            log.info("Requested month {}-{} is in the past — return empty", year, month);
//            return List.of();
//        }

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
                throw new IllegalStateException(
                        String.format("Cannot remove coach %d from date %s because some slots are not available (may be booked or in progress).",
                                coachId, date)
                );
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

    @Override
    public List<SlotAvailableResponse> getAvailableSlots(int coachId, LocalDate date) {
        if (!coachRepository.existsById(coachId)) {
            throw new IllegalArgumentException("Coach không tồn tại.");
        }

        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) throw new IllegalArgumentException("Ngày không được nhỏ hơn ngày hiện tại ");
        LocalDateTime now = LocalDateTime.now();
        return coachWorkScheduleRepository
                .findAllByCoachIdAndDateAndStatusWithSlot(coachId, date, CoachWorkScheduleStatus.AVAILABLE)
                .stream()
                .filter(cws -> {
                    if (date.isEqual(today)) {
                        LocalDateTime slotStart = LocalDateTime.of(date, cws.getSlot().getStartTime());
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
        // Resolve coach by accountId (FE gửi accountId)
        var coachOpt = coachRepository.findByAccountId(accountId);
        if (coachOpt.isEmpty()) {
            throw new IllegalArgumentException("Not exist account");
        }
        int coachId = coachOpt.get().getId();

        YearMonth requested = YearMonth.of(year, month);
        LocalDate start = requested.atDay(1);           // bắt đầu từ ngày 1 của tháng (kể cả quá khứ)
        LocalDate end = requested.atEndOfMonth();      // tới cuối tháng

        // Lấy tất cả CoachWorkSchedule trong khoảng và lọc theo coachId
        List<CoachWorkSchedule> schedules = coachWorkScheduleRepository.findAllByDateBetweenWithCoach(start, end);

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
    public List<CoachSummaryDTO> findAvailableCoaches(LocalDate date, int slotId, Integer excludeCoachId) {
        if (date == null) throw new IllegalArgumentException("date is required");
        if (slotId <= 0) throw new IllegalArgumentException("invalid slotId");

        // query all CWS for this date + slot (include coach join)
        List<CoachWorkSchedule> cwsList = coachWorkScheduleRepository.findAllByDateAndSlotIdWithCoach(date, slotId);

        return cwsList.stream()
                .filter(Objects::nonNull)
                .filter(cws -> cws.getStatus() == CoachWorkScheduleStatus.AVAILABLE)
                .filter(cws -> {
                    Coach coach = cws.getCoach();
                    return coach != null
                            && coach.getAccount() != null;
                })
                .filter(cws -> excludeCoachId == null || cws.getCoach().getId() != excludeCoachId)
                .map(CoachWorkSchedule::getCoach)
                .distinct()
                .map(coachMapper::toCoachSummaryDTO)
                .toList();
    }



}

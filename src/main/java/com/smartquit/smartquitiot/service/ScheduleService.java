package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.ScheduleAssignRequest;
import com.smartquit.smartquitiot.dto.request.ScheduleUpdateRequest;
import com.smartquit.smartquitiot.dto.response.ScheduleByDayResponse;
import com.smartquit.smartquitiot.dto.response.SlotAvailableResponse;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {
    int assignCoachesToDates(ScheduleAssignRequest request);
    List<ScheduleByDayResponse> getSchedulesByMonth(int year, int month);
    void updateScheduleByDate(LocalDate date, ScheduleUpdateRequest request);
    List<SlotAvailableResponse> getAvailableSlots(int coachId, LocalDate date);
    List<LocalDate> getWorkdaysByMonth(int accountId, int year, int month);

}

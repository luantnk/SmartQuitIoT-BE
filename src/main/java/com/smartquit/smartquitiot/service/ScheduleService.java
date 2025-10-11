package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.ScheduleAssignRequest;

public interface ScheduleService {
    int assignCoachesToDates(ScheduleAssignRequest request);
}

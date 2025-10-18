package com.smartquit.smartquitiot.service;


import com.smartquit.smartquitiot.dto.request.AppointmentRequest;
import com.smartquit.smartquitiot.dto.response.AppointmentResponse;

import java.util.List;

public interface AppointmentService {

    // Member đặt slot với coach
    AppointmentResponse bookAppointment( AppointmentRequest request);
    // Hủy lịch đã đặt
    void cancelAppointment(int appointmentId, int memberId);
    List<AppointmentResponse> getAppointmentsByMemberId(int memberId);
}


package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.AppointmentRequest;
import com.smartquit.smartquitiot.dto.response.AppointmentResponse;
import com.smartquit.smartquitiot.dto.response.JoinTokenResponse;

import java.util.List;

public interface AppointmentService {

    AppointmentResponse bookAppointment(int memberId, AppointmentRequest request);

    void cancelAppointment(int appointmentId, int memberId);

    List<AppointmentResponse> getAppointmentsByMemberId(int memberId);

    JoinTokenResponse generateJoinTokenForAppointment(int appointmentId,int accountId);
}

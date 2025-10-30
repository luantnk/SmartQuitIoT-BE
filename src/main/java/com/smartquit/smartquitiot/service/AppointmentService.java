package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.AppointmentRequest;
import com.smartquit.smartquitiot.dto.response.AppointmentResponse;
import com.smartquit.smartquitiot.dto.response.JoinTokenResponse;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface AppointmentService {

    AppointmentResponse bookAppointment(int memberAccountId, AppointmentRequest request);

    void cancelAppointment(int appointmentId, int memberAccountId);

    void cancelAppointmentByCoach(int appointmentId, int coachAccountId);

    List<AppointmentResponse> getAppointmentsByMemberAccountId(int memberAccountId, String statusFilter, String dateFilter, int page, int size);

    List<AppointmentResponse> getAppointmentsByCoachAccountId(int coachAccountId, String statusFilter, String dateFilter, int page, int size);

    AppointmentResponse getAppointmentDetailForPrincipal(int appointmentId, int accountId);

    JoinTokenResponse generateJoinTokenForAppointment(int appointmentId, int accountId);
}

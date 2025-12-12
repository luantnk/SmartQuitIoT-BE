package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.AppointmentRequest;
import com.smartquit.smartquitiot.dto.response.AppointmentResponse;
import com.smartquit.smartquitiot.dto.response.JoinTokenResponse;
import com.smartquit.smartquitiot.dto.response.RemainingBookingResponse;
import com.smartquit.smartquitiot.enums.AppointmentStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AppointmentService {

    AppointmentResponse bookAppointment(int memberAccountId, AppointmentRequest request);

    void cancelAppointment(int appointmentId, int memberAccountId);

    void cancelAppointmentByCoach(int appointmentId, int coachAccountId);

    List<AppointmentResponse> getAppointmentsByMemberAccountId(int memberAccountId, String statusFilter, String dateFilter, int page, int size);

    List<AppointmentResponse> getAppointmentsByCoachAccountId(int coachAccountId, String statusFilter, String dateFilter, int page, int size);

    AppointmentResponse getAppointmentDetailForPrincipal(int appointmentId, int accountId);

    JoinTokenResponse generateJoinTokenForAppointment(int appointmentId, int accountId);

    RemainingBookingResponse getRemainingBookingsForMember(int memberAccountId);

    void completeAppointmentByCoach(int appointmentId, int coachAccountId);

    void addSnapshots(int appointmentId, int accountId, List<String> urls);

    List<String> getSnapshots(int appointmentId, int accountId);

    Page<AppointmentResponse> getAllAppointments(int page, int size, AppointmentStatus status);

    void reassignAppointment(int appointmentId, int targetCoachId);
}

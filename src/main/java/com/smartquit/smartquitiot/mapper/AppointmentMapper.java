package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.AppointmentResponse;
import com.smartquit.smartquitiot.entity.Appointment;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public AppointmentResponse toResponse(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        return AppointmentResponse.builder()
                .appointmentId(appointment.getId())
                .coachId(appointment.getCoach().getId())
                .coachName(appointment.getCoach().getFirstName() + " " + appointment.getCoach().getLastName())
                .date(appointment.getDate())
                .slotId(appointment.getSlot().getId())
                .startTime(appointment.getSlot().getStartTime())
                .endTime(appointment.getSlot().getEndTime())
                .build();
    }

    public AppointmentResponse toResponseWithRuntime(Appointment appointment, String runtimeStatus) {
        AppointmentResponse response = toResponse(appointment);
        response.setRuntimeStatus(runtimeStatus);
        return response;
    }
}

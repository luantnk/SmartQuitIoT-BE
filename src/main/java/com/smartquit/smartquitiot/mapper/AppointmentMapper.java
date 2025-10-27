package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.AppointmentResponse;
import com.smartquit.smartquitiot.entity.Appointment;
import org.springframework.stereotype.Component;

import java.time.*;

@Component
public class AppointmentMapper {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public AppointmentResponse toResponse(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        Integer appointmentId = appointment.getId();
        Integer coachId = appointment.getCoach() != null ? appointment.getCoach().getId() : null;
        String coachName = "";
        if (appointment.getCoach() != null) {
            String fn = appointment.getCoach().getFirstName() != null ? appointment.getCoach().getFirstName() : "";
            String ln = appointment.getCoach().getLastName() != null ? appointment.getCoach().getLastName() : "";
            coachName = (fn + " " + ln).trim();
        }

        LocalDate date = appointment.getDate();
        Integer slotId = appointment.getSlot() != null ? appointment.getSlot().getId() : null;
        LocalTime startTime = appointment.getSlot() != null ? appointment.getSlot().getStartTime() : null;
        LocalTime endTime = appointment.getSlot() != null ? appointment.getSlot().getEndTime() : null;

        AppointmentResponse.AppointmentResponseBuilder builder = AppointmentResponse.builder()
                .appointmentId(appointmentId != null ? appointmentId : 0)
                .coachId(coachId != null ? coachId : 0)
                .coachName(coachName)
                .date(date)
                .slotId(slotId != null ? slotId : 0)
                .startTime(startTime)
                .endTime(endTime);

        // taạo channel và meeting url
        if (appointmentId != null) {
            String channel = "appointment_" + appointmentId;
            String meetingUrl = "/meeting/" + appointmentId;
            builder.channelName(channel).meetingUrl(meetingUrl);
        }

        // compute join window instants if date/start/end present
        if (date != null && startTime != null && endTime != null) {
            LocalDateTime windowStartLdt = LocalDateTime.of(date, startTime).minusMinutes(5);
            LocalDateTime windowEndLdt   = LocalDateTime.of(date, endTime).plusMinutes(5);

            Instant windowStartInstant = windowStartLdt.atZone(ZONE).toInstant();
            Instant windowEndInstant   = windowEndLdt.atZone(ZONE).toInstant();

            builder.joinWindowStart(windowStartInstant)
                    .joinWindowEnd(windowEndInstant);
        }

        return builder.build();
    }

    public AppointmentResponse toResponseWithRuntime(Appointment appointment, String runtimeStatus) {
        AppointmentResponse response = toResponse(appointment);
        if (response != null) {
            response.setRuntimeStatus(runtimeStatus);
        }
        return response;
    }
}

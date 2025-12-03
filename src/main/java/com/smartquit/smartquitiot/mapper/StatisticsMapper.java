package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.UpcomingAppointmentDTO;
import com.smartquit.smartquitiot.entity.Appointment;
import com.smartquit.smartquitiot.enums.AppointmentStatus;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class StatisticsMapper {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public UpcomingAppointmentDTO toUpcomingAppointmentDTO(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        int appointmentId = appointment.getId();
        
        // Map member info
        int memberId = 0;
        String memberName = "";
        String memberAvatarUrl = null;
        
        if (appointment.getMember() != null) {
            memberId = appointment.getMember().getId();
            String firstName = appointment.getMember().getFirstName() != null 
                    ? appointment.getMember().getFirstName() : "";
            String lastName = appointment.getMember().getLastName() != null 
                    ? appointment.getMember().getLastName() : "";
            memberName = (firstName + " " + lastName).trim();
            memberAvatarUrl = appointment.getMember().getAvatarUrl();
        }

        // Map time from slot
        String time = "";
        if (appointment.getCoachWorkSchedule() != null 
                && appointment.getCoachWorkSchedule().getSlot() != null) {
            time = appointment.getCoachWorkSchedule().getSlot().getStartTime()
                    .format(TIME_FORMATTER);
        }

        // Map status
        String status = "unknown";
        if (appointment.getAppointmentStatus() != null) {
            AppointmentStatus appointmentStatus = appointment.getAppointmentStatus();
            status = appointmentStatus.name().toLowerCase();
        }

        return UpcomingAppointmentDTO.builder()
                .appointmentId(appointmentId)
                .memberId(memberId)
                .memberName(memberName)
                .memberAvatarUrl(memberAvatarUrl)
                .time(time)
                .status(status)
                .build();
    }

    public List<UpcomingAppointmentDTO> toUpcomingAppointmentDTOList(List<Appointment> appointments) {
        if (appointments == null) {
            return List.of();
        }
        return appointments.stream()
                .map(this::toUpcomingAppointmentDTO)
                .collect(Collectors.toList());
    }
}

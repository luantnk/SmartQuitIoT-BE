package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.FeedbackResponse;
import com.smartquit.smartquitiot.entity.Appointment;
import com.smartquit.smartquitiot.entity.CoachWorkSchedule;
import com.smartquit.smartquitiot.entity.Feedback;
import com.smartquit.smartquitiot.entity.Slot;

import java.time.LocalDate;
import java.time.LocalTime;

public class FeedbackMapper {

    public static FeedbackResponse toResponse(Feedback f) {
        if (f == null) return null;

        // member name & avatar
        String memberName = null;
        String avatarUrl = null;
        if (f.getMember() != null) {
            String fn = f.getMember().getFirstName() == null ? "" : f.getMember().getFirstName();
            String ln = f.getMember().getLastName() == null ? "" : f.getMember().getLastName();
            memberName = (fn + " " + ln).trim();
            avatarUrl = f.getMember().getAvatarUrl();
        }

        // appointment date and slot
        LocalDate appointmentDate = null;
        LocalTime startTime = null;
        LocalTime endTime = null;
        Appointment ap = f.getAppointment();
        if (ap != null) {
            appointmentDate = ap.getDate();
            CoachWorkSchedule cws = ap.getCoachWorkSchedule();
            if (cws != null && cws.getSlot() != null) {
                Slot s = cws.getSlot();
                startTime = s.getStartTime();
                endTime = s.getEndTime();
            }
        }

        return FeedbackResponse.builder()
                .id(f.getId())
                .memberName(memberName)
                .avatarUrl(avatarUrl)
                .date(f.getCreatedAt())
                .content(f.getContent())
                .rating(f.getStar())
                .appointmentDate(appointmentDate)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }
}

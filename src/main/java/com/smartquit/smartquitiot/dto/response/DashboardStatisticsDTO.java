package com.smartquit.smartquitiot.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardStatisticsDTO {
    // Summary cards
    int appointmentsToday;
    int appointmentsYesterday;
    int pendingRequests;
    int completedThisWeek;
    int completedLastWeek;
    int activeMembers;
    int newMembersThisMonth;
    
    // Upcoming appointments
    List<UpcomingAppointmentDTO> upcomingAppointments;
}

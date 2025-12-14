package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.DashboardStatisticsDTO;
import com.smartquit.smartquitiot.entity.Coach;
import com.smartquit.smartquitiot.mapper.StatisticsMapper;
import com.smartquit.smartquitiot.repository.AppointmentRepository;
import com.smartquit.smartquitiot.repository.MemberRepository;
import com.smartquit.smartquitiot.mapper.StatisticsMapper;
import com.smartquit.smartquitiot.service.CoachService;
import com.smartquit.smartquitiot.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final AppointmentRepository appointmentRepository;
    private final MemberRepository memberRepository;
    private final StatisticsMapper statisticsMapper;
    private final CoachService coachService;

    @Override
    public DashboardStatisticsDTO getDashboardStatistics() {
        // Lấy coach hiện tại đang đăng nhập
        Coach currentCoach = coachService.getAuthenticatedCoach();
        int coachId = currentCoach.getId();

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        // Tính start và end của tuần hiện tại (Monday to Sunday)
        int dayOfWeek = today.getDayOfWeek().getValue(); // 1 = Monday, 7 = Sunday
        LocalDate startOfWeek = today.minusDays(dayOfWeek - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        
        // Tính start và end của tuần trước
        LocalDate startOfLastWeek = startOfWeek.minusDays(7);
        LocalDate endOfLastWeek = startOfLastWeek.plusDays(6);
        
        // Tính start của tháng hiện tại
        LocalDateTime startOfMonth = LocalDateTime.of(today.withDayOfMonth(1), LocalTime.MIN);

        // Appointments Today (filter theo coach)
        long appointmentsToday = appointmentRepository.countByDateAndCoachId(today, coachId);
        long appointmentsYesterday = appointmentRepository.countByDateAndCoachId(yesterday, coachId);
        
        // Pending Requests (filter theo coach)
        long pendingRequests = appointmentRepository.countPendingRequestsByCoachId(coachId);
        
        // Completed This Week (filter theo coach)
        long completedThisWeek = appointmentRepository.countCompletedBetweenByCoachId(coachId, startOfWeek, endOfWeek);
        long completedLastWeek = appointmentRepository.countCompletedBetweenByCoachId(coachId, startOfLastWeek, endOfLastWeek);
        
        // Active Members (filter theo coach - members đã có appointment với coach này)
        long activeMembers = memberRepository.countActiveMembersByCoachId(coachId);
        long newMembersThisMonth = memberRepository.countNewMembersByCoachIdSince(coachId, startOfMonth);
        
        // Upcoming Appointments (filter theo coach)
        var upcomingAppointments = appointmentRepository.findUpcomingByDateAndCoachId(today, coachId);
        var upcomingDTOs = statisticsMapper.toUpcomingAppointmentDTOList(upcomingAppointments);

        return DashboardStatisticsDTO.builder()
                .appointmentsToday((int) appointmentsToday)
                .appointmentsYesterday((int) appointmentsYesterday)
                .pendingRequests((int) pendingRequests)
                .completedThisWeek((int) completedThisWeek)
                .completedLastWeek((int) completedLastWeek)
                .activeMembers((int) activeMembers)
                .newMembersThisMonth((int) newMembersThisMonth)
                .upcomingAppointments(upcomingDTOs)
                .build();
    }
}

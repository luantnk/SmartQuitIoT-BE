package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.entity.Phase;
import com.smartquit.smartquitiot.entity.PhaseDetail;
import com.smartquit.smartquitiot.entity.QuitPlan;
import com.smartquit.smartquitiot.repository.PhaseDetailRepository;
import com.smartquit.smartquitiot.repository.PhaseRepository;
import com.smartquit.smartquitiot.service.PhaseDetailService;
import com.smartquit.smartquitiot.service.ReminderQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhaseDetailServiceImpl implements PhaseDetailService {
    private final PhaseRepository phaseRepository;
    private final PhaseDetailRepository phaseDetailRepository;
    private final ReminderQueueService reminderQueueService;

    //sinh phase detail cho 1 phase bat ky, xoa cu tao moi
    @Override
    @Transactional
    public List<PhaseDetail> generatePhaseDetailsForPhase(Phase phase) {
        LocalDate start = phase.getStartDate();
        LocalDate end = phase.getEndDate();

        if (start == null || end == null) {
            throw new IllegalStateException("Phase " + phase.getName() + " dont have startDate or endDate");
        }
        // delete old
        //phaseDetailRepository.deleteByPhaseId(phase.getId());

        // create new each day
        List<PhaseDetail> details = new ArrayList<>();
        LocalDate current = start;
        int dayIndex = 1;

        while (!current.isAfter(end)) {
            PhaseDetail detail = new PhaseDetail();
            detail.setPhase(phase);
            detail.setDate(current);
            detail.setDayIndex(dayIndex);
            detail.setName("Day " +dayIndex);
            details.add(detail);
            current = current.plusDays(1);
            dayIndex++;
        }

        List<PhaseDetail> savedDetails = phaseDetailRepository.saveAll(details);
        //add reminder
        reminderQueueService.createDailyRemindersForPhase(phase, savedDetails);

        return savedDetails;
    }

    @Override
    @Transactional
    public List<PhaseDetail> generateInitialPhaseDetails(QuitPlan quitPlan, String phaseName) {
        Phase phase = phaseRepository
                .findByQuitPlan_IdAndName(quitPlan.getId(), phaseName)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy phase Preparation"));

        return  generatePhaseDetailsForPhase(phase);
    }

}

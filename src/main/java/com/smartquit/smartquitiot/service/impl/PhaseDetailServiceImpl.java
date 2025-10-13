package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.entity.Phase;
import com.smartquit.smartquitiot.entity.PhaseDetail;
import com.smartquit.smartquitiot.entity.QuitPlan;
import com.smartquit.smartquitiot.repository.PhaseDetailRepository;
import com.smartquit.smartquitiot.repository.PhaseRepository;
import com.smartquit.smartquitiot.service.PhaseDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PhaseDetailServiceImpl implements PhaseDetailService {
    private final PhaseRepository phaseRepository;
    private final PhaseDetailRepository phaseDetailRepository;


    //sinh phase detail cho 1 phase bat ky, xoa cu tao moi
    @Override
    @Transactional
    public void generatePhaseDetailsForPhase(Phase phase) {
        LocalDate start = phase.getStartDate();
        LocalDate end = phase.getEndDate();

        if (start == null || end == null) {
            throw new IllegalStateException("Phase " + phase.getName() + " dont have startDate or endDate");
        }

        long days = ChronoUnit.DAYS.between(start, end) + 1;

        // delete old if you have
        phaseDetailRepository.deleteByPhaseId(phase.getId());

        // create new each day
        List<PhaseDetail> details = new ArrayList<>();
        LocalDate current = start;
        int dayIndex = 1;

        while (!current.isAfter(end)) {
            PhaseDetail detail = new PhaseDetail();
            detail.setPhase(phase);
            detail.setDate(current);
            detail.setDayIndex(dayIndex);
            details.add(detail);
            current = current.plusDays(1);
            dayIndex++;
        }

        phaseDetailRepository.saveAll(details);
    }

    @Override
    @Transactional
    public void generateInitialPhaseDetails(QuitPlan quitPlan) {
        Phase prepPhase = phaseRepository
                .findByQuitPlan_IdAndName(quitPlan.getId(), "Preparation")
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy phase Preparation"));

        generatePhaseDetailsForPhase(prepPhase);
    }
}

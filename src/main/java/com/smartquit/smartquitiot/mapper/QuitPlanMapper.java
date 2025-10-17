package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.*;
import com.smartquit.smartquitiot.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuitPlanMapper {

    public QuitPlanResponse toResponse(QuitPlan plan) {
        if (plan == null) return null;

        QuitPlanResponse resp = new QuitPlanResponse();
        resp.setId(plan.getId());
        resp.setName(plan.getName());
        resp.setStatus(plan.getStatus());
        resp.setStartDate(plan.getStartDate());
        resp.setEndDate(plan.getEndDate());
        resp.setUseNRT(plan.isUseNRT());
        resp.setFtndScore(plan.getFtndScore());

        resp.setFormMetricDTO(toFormMetricDTO(plan.getFormMetric()));

        if (plan.getPhases() != null) {
            List<PhaseDTO> phaseDTOs = plan.getPhases().stream()
                    .map(this::toPhaseDTO)
                    .toList();
            resp.setPhases(phaseDTOs);
        }

        return resp;
    }

    private FormMetricDTO toFormMetricDTO(FormMetric metric) {
        if (metric == null) return null;

        FormMetricDTO dto = new FormMetricDTO();
        dto.setId(metric.getId());
        dto.setSmokeAvgPerDay(metric.getSmokeAvgPerDay());
        dto.setNumberOfYearsOfSmoking(metric.getNumberOfYearsOfSmoking());
        dto.setCigarettesPerPackage(metric.getCigarettesPerPackage());
        dto.setMinutesAfterWakingToSmoke(metric.getMinutesAfterWakingToSmoke());
        dto.setSmokingInForbiddenPlaces(metric.isSmokingInForbiddenPlaces());
        dto.setCigaretteHateToGiveUp(metric.isCigaretteHateToGiveUp());
        dto.setMorningSmokingFrequency(metric.isMorningSmokingFrequency());
        dto.setSmokeWhenSick(metric.isSmokeWhenSick());
        dto.setMoneyPerPackage(metric.getMoneyPerPackage());
        dto.setEstimatedMoneySavedOnPlan(metric.getEstimatedMoneySavedOnPlan());
        dto.setAmountOfNicotinePerCigarettes(metric.getAmountOfNicotinePerCigarettes());
        dto.setEstimatedNicotineIntakePerDay(metric.getEstimatedNicotineIntakePerDay());
        dto.setInterests(metric.getInterests());
        dto.setTriggered(metric.getTriggered());
        return dto;
    }

    private PhaseDTO toPhaseDTO(Phase phase) {
        if (phase == null) return null;

        PhaseDTO dto = new PhaseDTO();
        dto.setId(phase.getId());
        dto.setName(phase.getName());
        dto.setDurationDay(phase.getDurationDays());
        dto.setReason(phase.getReason());
        dto.setStartDate(phase.getStartDate());
        dto.setEndDate(phase.getEndDate());

        if (phase.getDetails() != null) {
            List<PhaseDetailResponseDTO> details = phase.getDetails().stream()
                    .map(this::toPhaseDetailDTO)
                    .toList();
            dto.setDetails(details);
        }

        return dto;
    }

    private PhaseDetailResponseDTO toPhaseDetailDTO(PhaseDetail detail) {
        if (detail == null) return null;

        PhaseDetailResponseDTO dto = new PhaseDetailResponseDTO();
        dto.setId(detail.getId());
        dto.setName(detail.getName());
        dto.setDate(detail.getDate());
        dto.setDayIndex(detail.getDayIndex());

        if (detail.getPhaseDetailMissions() != null) {
            List<PhaseDetailMissionResponseDTO> missions = detail.getPhaseDetailMissions().stream()
                    .map(this::toMissionDTO)
                    .toList();
            dto.setMissions(missions);
        }

        return dto;
    }

    private PhaseDetailMissionResponseDTO toMissionDTO(PhaseDetailMission mission) {
        if (mission == null) return null;

        PhaseDetailMissionResponseDTO dto = new PhaseDetailMissionResponseDTO();
        dto.setId(mission.getId());
        dto.setCode(mission.getCode());
        dto.setName(mission.getName());
        dto.setDescription(mission.getDescription());
        dto.setStatus(mission.getStatus());
        return dto;
    }
}

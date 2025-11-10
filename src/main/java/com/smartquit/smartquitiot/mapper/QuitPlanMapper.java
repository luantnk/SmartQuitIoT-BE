package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.*;
import com.smartquit.smartquitiot.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

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

    public QuitPlanResponse toQuitPlanResponse(QuitPlan plan) {
        if (plan == null) return null;

        QuitPlanResponse resp = new QuitPlanResponse();
        resp.setId(plan.getId());
        resp.setName(plan.getName());
        resp.setStatus(plan.getStatus());
        resp.setStartDate(plan.getStartDate());
        resp.setEndDate(plan.getEndDate());
        resp.setUseNRT(plan.isUseNRT());
        resp.setFtndScore(plan.getFtndScore());
        resp.setCreatedAt(plan.getCreatedAt());
        resp.setActive(plan.isActive());
        resp.setFormMetricDTO(toFormMetricDTO(plan.getFormMetric()));

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
        dto.setCondition(phase.getCondition());
        dto.setTotalMissions(phase.getTotalMissions());
        dto.setCompletedMissions(phase.getCompletedMissions());
        dto.setProgress(phase.getProgress());
        dto.setStatus(phase.getStatus());
        dto.setCompletedAt(phase.getCompletedAt());
        dto.setFm_cigarettes_total(phase.getFm_cigarettes_total());
        dto.setAvg_cigarettes(phase.getAvg_cigarettes());
        dto.setAvg_craving_level(phase.getAvg_craving_level());
        dto.setKeepPhase(phase.isKeepPhase());
        dto.setCreateAt(phase.getCreatedAt());

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
    public List<QuitPlanResponse> toViewAll(List<QuitPlan> quitPlans) {
        if (quitPlans == null || quitPlans.isEmpty()) return List.of();

        return quitPlans.stream().map(plan -> {
            QuitPlanResponse resp = new QuitPlanResponse();
            resp.setId(plan.getId());
            resp.setName(plan.getName());
            resp.setStatus(plan.getStatus());
            resp.setStartDate(plan.getStartDate());
            resp.setEndDate(plan.getEndDate());
            resp.setUseNRT(plan.isUseNRT());
            resp.setFtndScore(plan.getFtndScore());
            resp.setCreatedAt(plan.getCreatedAt());
            resp.setActive(plan.isActive());
           // resp.setFormMetricDTO(toFormMetricDTO(plan.getFormMetric()));
            return resp;
        }).collect(Collectors.toList());
    }


}

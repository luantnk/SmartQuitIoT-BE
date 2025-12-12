package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.FormMetricDTO;
import com.smartquit.smartquitiot.dto.response.GetFormMetricResponse;
import com.smartquit.smartquitiot.dto.response.UpdateFormMetricResponse;
import com.smartquit.smartquitiot.entity.FormMetric;
import org.springframework.stereotype.Component;


@Component
public class FormMetricMapper {


    public FormMetricDTO toDTO(FormMetric formMetric) {
        FormMetricDTO formMetricDTO = new FormMetricDTO();
        formMetricDTO.setId(formMetric.getId());
        formMetricDTO.setSmokeAvgPerDay(formMetric.getSmokeAvgPerDay());
        formMetricDTO.setNumberOfYearsOfSmoking(formMetric.getNumberOfYearsOfSmoking());
        formMetricDTO.setCigarettesPerPackage(formMetric.getCigarettesPerPackage());
        formMetricDTO.setMinutesAfterWakingToSmoke(formMetric.getMinutesAfterWakingToSmoke());
        formMetricDTO.setSmokingInForbiddenPlaces(formMetric.isSmokingInForbiddenPlaces());
        formMetricDTO.setCigaretteHateToGiveUp(formMetric.isCigaretteHateToGiveUp());
        formMetricDTO.setMorningSmokingFrequency(formMetric.isMorningSmokingFrequency());
        formMetricDTO.setSmokeWhenSick(formMetric.isSmokeWhenSick());
        formMetricDTO.setMoneyPerPackage(formMetric.getMoneyPerPackage());
        formMetricDTO.setEstimatedMoneySavedOnPlan(formMetric.getEstimatedMoneySavedOnPlan());
        formMetricDTO.setEstimatedNicotineIntakePerDay(formMetric.getEstimatedNicotineIntakePerDay());
        formMetricDTO.setAmountOfNicotinePerCigarettes(formMetric.getAmountOfNicotinePerCigarettes());
        formMetricDTO.setInterests(formMetric.getInterests());
        formMetricDTO.setTriggered(formMetric.getTriggered());

        return formMetricDTO;
    }

    public GetFormMetricResponse toDTOByGetFormMetric(FormMetric formMetric, int ftndScore) {
        GetFormMetricResponse getFormMetricResponse = new GetFormMetricResponse();

        FormMetricDTO formMetricDTO = new FormMetricDTO();
        formMetricDTO.setId(formMetric.getId());
        formMetricDTO.setSmokeAvgPerDay(formMetric.getSmokeAvgPerDay());
        formMetricDTO.setNumberOfYearsOfSmoking(formMetric.getNumberOfYearsOfSmoking());
        formMetricDTO.setCigarettesPerPackage(formMetric.getCigarettesPerPackage());
        formMetricDTO.setMinutesAfterWakingToSmoke(formMetric.getMinutesAfterWakingToSmoke());
        formMetricDTO.setSmokingInForbiddenPlaces(formMetric.isSmokingInForbiddenPlaces());
        formMetricDTO.setCigaretteHateToGiveUp(formMetric.isCigaretteHateToGiveUp());
        formMetricDTO.setMorningSmokingFrequency(formMetric.isSmokeWhenSick());
        formMetricDTO.setMoneyPerPackage(formMetric.getMoneyPerPackage());
        formMetricDTO.setEstimatedMoneySavedOnPlan(formMetric.getEstimatedMoneySavedOnPlan());
        formMetricDTO.setEstimatedNicotineIntakePerDay(formMetric.getEstimatedNicotineIntakePerDay());
        formMetricDTO.setAmountOfNicotinePerCigarettes(formMetric.getAmountOfNicotinePerCigarettes());
        formMetricDTO.setInterests(formMetric.getInterests());
        formMetricDTO.setTriggered(formMetric.getTriggered());

        getFormMetricResponse.setFormMetricDTO(formMetricDTO);
        getFormMetricResponse.setFtnd_score(ftndScore);
        return getFormMetricResponse;
    }

    public UpdateFormMetricResponse toDTOByUpdateFormMetric(FormMetric formMetric, int ftndScore, boolean alert) {
        UpdateFormMetricResponse updateFormMetricResponse = new UpdateFormMetricResponse();

        FormMetricDTO formMetricDTO = new FormMetricDTO();
        formMetricDTO.setId(formMetric.getId());
        formMetricDTO.setSmokeAvgPerDay(formMetric.getSmokeAvgPerDay());
        formMetricDTO.setNumberOfYearsOfSmoking(formMetric.getNumberOfYearsOfSmoking());
        formMetricDTO.setCigarettesPerPackage(formMetric.getCigarettesPerPackage());
        formMetricDTO.setMinutesAfterWakingToSmoke(formMetric.getMinutesAfterWakingToSmoke());
        formMetricDTO.setSmokingInForbiddenPlaces(formMetric.isSmokingInForbiddenPlaces());
        formMetricDTO.setCigaretteHateToGiveUp(formMetric.isCigaretteHateToGiveUp());
        formMetricDTO.setMorningSmokingFrequency(formMetric.isSmokeWhenSick());
        formMetricDTO.setMoneyPerPackage(formMetric.getMoneyPerPackage());
        formMetricDTO.setEstimatedMoneySavedOnPlan(formMetric.getEstimatedMoneySavedOnPlan());
        formMetricDTO.setEstimatedNicotineIntakePerDay(formMetric.getEstimatedNicotineIntakePerDay());
        formMetricDTO.setAmountOfNicotinePerCigarettes(formMetric.getAmountOfNicotinePerCigarettes());
        formMetricDTO.setInterests(formMetric.getInterests());
        formMetricDTO.setTriggered(formMetric.getTriggered());

        updateFormMetricResponse.setFormMetricDTO(formMetricDTO);
        updateFormMetricResponse.setFntd_score(ftndScore);
        updateFormMetricResponse.setAlert(alert);

        return updateFormMetricResponse;
    }
}

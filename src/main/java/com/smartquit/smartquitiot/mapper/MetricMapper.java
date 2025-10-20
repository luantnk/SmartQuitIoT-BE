package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.MetricDTO;
import com.smartquit.smartquitiot.entity.Metric;
import org.springframework.stereotype.Component;

@Component
public class MetricMapper {

    //User for home screen metric statistics
    public MetricDTO toMetricStatistic(Metric metric){
        MetricDTO metricDTO = new MetricDTO();
        if(metric == null) return null;
        metricDTO.setStreaks(metric.getStreaks());
        metricDTO.setAnnualSaved(metric.getAnnualSaved());
        metricDTO.setMoneySaved(metric.getMoneySaved());
        metricDTO.setReductionPercentage(metric.getReductionPercentage());
        metricDTO.setSmokeFreeDayPercentage(metric.getSmokeFreeDayPercentage());

        return metricDTO;
    }

}

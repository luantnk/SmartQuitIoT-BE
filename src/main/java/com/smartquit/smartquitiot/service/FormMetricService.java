package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.UpdateFormMetricRequest;
import com.smartquit.smartquitiot.dto.response.FormMetricDTO;
import com.smartquit.smartquitiot.dto.response.GetFormMetricResponse;
import com.smartquit.smartquitiot.dto.response.UpdateFormMetricResponse;

public interface FormMetricService{
    GetFormMetricResponse getMyFormMetric();
    UpdateFormMetricResponse updateMyFormMetric(UpdateFormMetricRequest req);
}

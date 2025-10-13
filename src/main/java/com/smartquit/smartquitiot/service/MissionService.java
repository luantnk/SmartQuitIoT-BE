package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.FormMetric;
import com.smartquit.smartquitiot.entity.Mission;
import com.smartquit.smartquitiot.entity.PhaseDetail;

import java.util.List;

public interface MissionService {
    List<Mission> filterMissionsForPhaseDetail(PhaseDetail detail, FormMetric metric, Account account);
}

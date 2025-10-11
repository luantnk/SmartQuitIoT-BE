package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.response.CoachDTO;
import com.smartquit.smartquitiot.dto.response.CoachSummaryDTO;
import com.smartquit.smartquitiot.entity.Coach;

import java.util.List;

public interface    CoachService {

    Coach getAuthenticatedCoach();
    List<CoachSummaryDTO> getCoachList();
}

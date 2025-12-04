package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.CoachUpdateRequest;
import com.smartquit.smartquitiot.dto.response.CoachDTO;
import com.smartquit.smartquitiot.dto.response.CoachSummaryDTO;
import com.smartquit.smartquitiot.entity.Coach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

public interface    CoachService {

    Coach getAuthenticatedCoach();
    CoachDTO getAuthenticatedCoachProfile();
    List<CoachSummaryDTO> getCoachList();
    Page<CoachDTO> getAllCoaches(int page, int size, String searchString, Sort.Direction sortBy, Boolean isActive);
    CoachDTO getCoachById(int id);
    CoachDTO updateProfile(int coachId, CoachUpdateRequest request);
    Map<String, Object> getCoachStatistics();
}

package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.CreateAchievementRequest;
import com.smartquit.smartquitiot.dto.response.AchievementDTO;
import org.springframework.data.domain.Page;

public interface AchievementService {

    Page<AchievementDTO> getAllAchievements(int page, int size, String search);
    AchievementDTO getAchievementById(int id);

    AchievementDTO createAchievement(CreateAchievementRequest request);

    AchievementDTO deleteAchievement(int id);

    AchievementDTO updateAchievement(int id, CreateAchievementRequest request);
}

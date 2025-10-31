package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.AddAchievementRequest;
import com.smartquit.smartquitiot.dto.response.AchievementDTO;
import com.smartquit.smartquitiot.dto.response.TopMemberAchievementDTO;
import com.smartquit.smartquitiot.entity.Achievement;

import java.util.List;
import java.util.Optional;

public interface MemberAchievementService {

    Optional<Achievement> addMemberAchievement(AddAchievementRequest request);
    List<AchievementDTO> getAllMyAchievements();
    List<TopMemberAchievementDTO> getTop10MembersWithAchievements();
    List<AchievementDTO> getMyAchievementsAtHome();

}

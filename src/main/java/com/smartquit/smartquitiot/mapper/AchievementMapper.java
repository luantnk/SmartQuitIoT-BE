package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.AchievementDTO;
import com.smartquit.smartquitiot.entity.Achievement;
import org.springframework.stereotype.Component;

@Component
public class AchievementMapper {

    public AchievementDTO toAchievementDTO(Achievement achievement) {
        if(achievement == null) {
            return null;
        }
        AchievementDTO achievementDTO = new AchievementDTO();
        achievementDTO.setId(achievement.getId());
        achievementDTO.setName(achievement.getName());
        achievementDTO.setDescription(achievement.getDescription());
        achievementDTO.setIcon(achievement.getIcon());
        achievementDTO.setType(achievement.getType().name());
        achievementDTO.setCondition(achievement.getCondition());
        achievementDTO.setCreatedAt(achievement.getCreatedAt());
        achievementDTO.setUpdatedAt(achievement.getUpdatedAt());
        return achievementDTO;
    }

}

package com.smartquit.smartquitiot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartquit.smartquitiot.dto.request.CreateAchievementRequest;
import com.smartquit.smartquitiot.dto.response.AchievementDTO;
import com.smartquit.smartquitiot.entity.Achievement;
import com.smartquit.smartquitiot.enums.AchievementType;
import com.smartquit.smartquitiot.mapper.AchievementMapper;
import com.smartquit.smartquitiot.repository.AchievementRepository;
import com.smartquit.smartquitiot.service.AchievementService;
import com.smartquit.smartquitiot.service.NotificationService;
import com.smartquit.smartquitiot.specifications.AchievementSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementRepository achievementRepository;
    private final AchievementMapper achievementMapper;
    private final NotificationService notificationService;



    @Override
    public Page<AchievementDTO> getAllAchievements(int page, int size, String search) {
        Pageable pageRequest = PageRequest.of(page, size);
        Specification<Achievement> spec = Specification.allOf(
                AchievementSpecification.hasSearchString(search),
                (root, query, cb) -> cb.equal(root.get("isDeleted"), false)
        );
        Page<Achievement> achievements = achievementRepository.findAll(spec, pageRequest);
        return achievements.map(achievementMapper::toAchievementDTO);
    }

    @Override
    public AchievementDTO getAchievementById(int id) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Achievement with id " + id + " not found!"));
        return achievementMapper.toAchievementDTO(achievement);
    }

    @Override
    public AchievementDTO createAchievement(CreateAchievementRequest request) {
        validateCondition(request.getCondition());

        Achievement achievement = new Achievement();
        achievement.setName(request.getName());
        achievement.setDescription(request.getDescription());
        achievement.setIcon(request.getIcon());
        achievement.setType(AchievementType.valueOf(request.getType()));
        achievement.setCondition(request.getCondition());

        notificationService.sendSystemActivityNotification("New achievement created: " + request.getName(), "A new achievement has been added to the system. Check it out!");

        return achievementMapper.toAchievementDTO(achievementRepository.save(achievement));
    }

    @Override
    public AchievementDTO deleteAchievement(int id) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Achievement not found with id: " + id));

        achievement.setDeleted(true);

        return achievementMapper.toAchievementDTO(achievementRepository.save(achievement));
    }

    @Override
    public AchievementDTO updateAchievement(int id, CreateAchievementRequest request) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Achievement not found"));

        validateCondition(request.getCondition());

        achievement.setName(request.getName());
        achievement.setDescription(request.getDescription());
        achievement.setIcon(request.getIcon());
        achievement.setType(AchievementType.valueOf(request.getType()));
        achievement.setCondition(request.getCondition());

        return achievementMapper.toAchievementDTO(achievementRepository.save(achievement));
    }

    private void validateCondition(JsonNode condition) {
        if (!condition.has("field") || !condition.has("operator") || !condition.has("value")) {
            throw new IllegalArgumentException("Condition must have field, operator, and value");
        }

        String field = condition.get("field").asText();
        String operator = condition.get("operator").asText();

        // Validate field
        List<String> validFields = Arrays.asList(
                "streaks", "steps", "money_saved", "post_count",
                "comment_count", "total_mission_completed", "completed_all_mission_in_day"
        );
        if (!validFields.contains(field)) {
            throw new IllegalArgumentException("Invalid condition field: " + field);
        }

        // Validate operator
        List<String> validOperators = Arrays.asList(">=", ">", "=", "<", "<=");
        if (!validOperators.contains(operator)) {
            throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    }
}

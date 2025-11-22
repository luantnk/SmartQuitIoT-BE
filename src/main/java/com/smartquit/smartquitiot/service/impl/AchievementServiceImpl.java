package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.AchievementDTO;
import com.smartquit.smartquitiot.entity.Achievement;
import com.smartquit.smartquitiot.mapper.AchievementMapper;
import com.smartquit.smartquitiot.repository.AchievementRepository;
import com.smartquit.smartquitiot.service.AchievementService;
import com.smartquit.smartquitiot.specifications.AchievementSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementRepository achievementRepository;
    private final AchievementMapper achievementMapper;


    @Override
    public Page<AchievementDTO> getAllAchievements(int page, int size, String search) {
        Pageable pageRequest = PageRequest.of(page, size);
        Specification<Achievement> spec = Specification.allOf(AchievementSpecification.hasSearchString(search));
        Page<Achievement> achievements = achievementRepository.findAll(spec, pageRequest);
        return achievements.map(achievementMapper::toAchievementDTO);
    }

    @Override
    public AchievementDTO getAchievementById(int id) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Achievement with id " + id + " not found!"));
        return achievementMapper.toAchievementDTO(achievement);
    }
}

package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.entity.Achievement;

import java.util.Optional;

public interface MemberAchievementService {

    Optional<Achievement> addAchievement(String field);

}

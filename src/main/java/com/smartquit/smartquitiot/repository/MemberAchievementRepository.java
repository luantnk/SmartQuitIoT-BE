package com.smartquit.smartquitiot.repository;


import com.smartquit.smartquitiot.entity.MemberAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberAchievementRepository extends JpaRepository<MemberAchievement, Integer> {
    List<MemberAchievement> getAchievements(int memberId);
    boolean existsByMember_IdAndAchievement_Id(int memberId, int achievementId);
}

package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Integer> {
}

package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Achievement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<Achievement, Integer> {

    Page<Achievement> findAll(Specification<Achievement> spec, Pageable packable);
}

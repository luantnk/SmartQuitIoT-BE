package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Achievement;
import com.smartquit.smartquitiot.util.agora.Packable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Integer> {

    Page<Achievement> findAll(Specification<Achievement> spec, Pageable packable);
}

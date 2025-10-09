package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Coach;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoachRepository extends JpaRepository<Coach, Integer> {
    Optional<Coach> findByAccountId(int accountId);
}

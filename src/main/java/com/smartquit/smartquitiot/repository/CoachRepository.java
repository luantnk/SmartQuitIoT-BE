package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Coach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoachRepository extends JpaRepository<Coach, Integer> {
    Optional<Coach> findByAccountId(int accountId);
    @EntityGraph(attributePaths = {"account"})
    List<Coach> findAllByAccountIsActiveTrueAndAccountIsBannedFalse();

    Page<Coach> findAll(Specification<Coach> spec, Pageable pageable);
}

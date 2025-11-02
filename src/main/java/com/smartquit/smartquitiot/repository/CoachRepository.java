package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Coach;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CoachRepository extends JpaRepository<Coach, Integer> {
    Optional<Coach> findByAccountId(int accountId);
    @EntityGraph(attributePaths = {"account"})
    List<Coach> findAllByAccountIsActiveTrueAndAccountIsBannedFalse();

    Page<Coach> findAll(Specification<Coach> spec, Pageable pageable);

    // Khoá trong khi đang update rating của coach ( từ Feedback)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coach c WHERE c.id = :id")
    Optional<Coach> findByIdForUpdate(@Param("id") int id);
}

package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Integer> {
    Optional<Member> findByAccountId(int accountId);

    Page<Member> findAll(Specification<Member> spec, Pageable pageable);
}

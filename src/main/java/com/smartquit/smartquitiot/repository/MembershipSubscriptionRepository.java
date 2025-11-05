package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.MembershipSubscription;
import com.smartquit.smartquitiot.enums.MembershipSubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MembershipSubscriptionRepository extends JpaRepository<MembershipSubscription , Integer> {

    Optional<MembershipSubscription> findByOrderCode(long orderCode);

    Optional<MembershipSubscription> findTopByMemberIdAndStatusOrderByCreatedAtDesc(int memberId, MembershipSubscriptionStatus status);

    @Query("SELECT ms FROM MembershipSubscription ms " +
            "WHERE ms.member.id = :memberId " +
            "  AND ms.startDate <= :now " +
            "  AND ms.endDate >= :now " +
            "  AND ms.status = com.smartquit.smartquitiot.enums.MembershipSubscriptionStatus.AVAILABLE")
    Optional<MembershipSubscription> findActiveByMemberId(@Param("memberId") int memberId, @Param("now") LocalDate now);

    List<MembershipSubscription> findByStatus(MembershipSubscriptionStatus status);

    Page<MembershipSubscription> findAll(Specification<MembershipSubscription> specification, Pageable pageable);
}

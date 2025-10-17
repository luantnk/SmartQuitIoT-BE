package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.MembershipSubscription;
import com.smartquit.smartquitiot.enums.MembershipSubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembershipSubscriptionRepository extends JpaRepository<MembershipSubscription , Integer> {

    Optional<MembershipSubscription> findByOrderCode(long orderCode);

    Optional<MembershipSubscription> findTopByMemberIdAndStatusOrderByCreatedAtDesc(int memberId, MembershipSubscriptionStatus status);
}

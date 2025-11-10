package com.smartquit.smartquitiot.repository;


import com.smartquit.smartquitiot.entity.QuitPlan;
import com.smartquit.smartquitiot.enums.QuitPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface QuitPlanRepository extends JpaRepository<QuitPlan,Integer> {
    List<QuitPlan> findByStatusIn(Collection<QuitPlanStatus> statuses);
    QuitPlan findTopByMemberIdOrderByCreatedAtDesc(Integer memberId);
    QuitPlan findByMember_IdAndStatus(int memberId, QuitPlanStatus status);
    QuitPlan findByMember_IdAndIsActiveTrue(int memberId);
    List<QuitPlan> findAllByMember_IdOrderByCreatedAtDesc(int memberId);
    Optional<QuitPlan> findByMember_IdAndId(int memberId, int quitPlanId);

}

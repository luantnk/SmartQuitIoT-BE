package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Integer>, JpaSpecificationExecutor<Member> {
    Optional<Member> findByAccountId(int accountId);

    @Override
    @EntityGraph(attributePaths = {"metric"})
    Page<Member> findAll(Specification<Member> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"metric", "quitPlans", "healthRecoveries", "account"})
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdWithRelations(@Param("id") int id);

    @EntityGraph(attributePaths = {"account"})
    List<Member> findAllByAccount_IsActiveTrueAndAccount_IsBannedFalse();

    @Query("SELECT COUNT(m) FROM Member m " +
           "JOIN m.account a " +
           "WHERE a.isActive = true AND a.isBanned = false")
    long countActiveMembers();

    @Query("SELECT COUNT(DISTINCT a.member) FROM Appointment a " +
           "JOIN a.member m " +
           "JOIN m.account acc " +
           "WHERE a.coach.id = :coachId " +
           "AND acc.isActive = true AND acc.isBanned = false " +
           "AND a.appointmentStatus <> com.smartquit.smartquitiot.enums.AppointmentStatus.CANCELLED")
    long countActiveMembersByCoachId(@Param("coachId") int coachId);

    @Query("SELECT COUNT(m) FROM Member m " +
           "JOIN m.account a " +
           "WHERE a.isActive = true AND a.isBanned = false " +
           "AND a.createdAt >= :startDate")
    long countNewMembersSince(@Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT COUNT(DISTINCT a.member) FROM Appointment a " +
           "JOIN a.member m " +
           "JOIN m.account acc " +
           "WHERE a.coach.id = :coachId " +
           "AND acc.isActive = true AND acc.isBanned = false " +
           "AND acc.createdAt >= :startDate " +
           "AND a.appointmentStatus <> com.smartquit.smartquitiot.enums.AppointmentStatus.CANCELLED")
    long countNewMembersByCoachIdSince(@Param("coachId") int coachId, 
                                      @Param("startDate") java.time.LocalDateTime startDate);
}

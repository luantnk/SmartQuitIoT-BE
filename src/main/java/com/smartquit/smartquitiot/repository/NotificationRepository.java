package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Notification;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification,Integer> {
    List<Notification> findByMember_IdOrderByCreatedAtDesc(int memberId);

    Page<Notification> findAll(Specification<Notification> spec, Pageable pageable);
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.id = :id AND n.member.id = :memberId AND n.isDeleted = false")
    int markOneRead(@Param("id") int id, @Param("memberId") int memberId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.member.id = :memberId AND n.isDeleted = false AND n.isRead = false")
    int markAllRead(@Param("memberId") int memberId);

    @Modifying
    @Query("UPDATE Notification n SET n.isDeleted = true " +
            "WHERE n.member.id = :memberId AND n.isDeleted = false")
    int deleteAll(@Param("memberId") int memberId);

    @Modifying
    @Query("UPDATE Notification n SET n.isDeleted = true " +
            "WHERE n.id = :id AND n.member.id = :memberId AND n.isDeleted = false")
    int deleteOne(@Param("id") int id, @Param("memberId") int memberId);

}

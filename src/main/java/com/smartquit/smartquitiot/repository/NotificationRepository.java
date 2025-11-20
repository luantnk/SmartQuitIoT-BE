package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Notification;
import com.smartquit.smartquitiot.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByAccount_IdOrderByCreatedAtDesc(int accountId);

    Page<Notification> findAll(Specification<Notification> spec, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.id = :id AND n.account.id = :accountId AND n.isDeleted = false")
    int markOneRead(@Param("id") int id, @Param("accountId") int accountId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.account.id = :accountId AND n.isDeleted = false AND n.isRead = false")
    int markAllRead(@Param("accountId") int accountId);

    @Modifying
    @Query("UPDATE Notification n SET n.isDeleted = true " +
            "WHERE n.account.id = :accountId AND n.isDeleted = false")
    int deleteAll(@Param("accountId") int accountId);

    @Modifying
    @Query("UPDATE Notification n SET n.isDeleted = true " +
            "WHERE n.id = :id AND n.account.id = :accountId AND n.isDeleted = false")
    int deleteOne(@Param("id") int id, @Param("accountId") int accountId);

    boolean existsByAccount_IdAndNotificationTypeAndDeepLinkAndIsDeletedFalse(
            int accountId, NotificationType notificationType, String deepLink
    );

    List<Notification> findAllByAccountIdAndIsDeletedFalseOrderByCreatedAtDesc(Integer accountId);

}

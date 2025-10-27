package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification,Integer> {
    List<Notification> findByMember_IdOrderByCreatedAtDesc(int memberId);
}

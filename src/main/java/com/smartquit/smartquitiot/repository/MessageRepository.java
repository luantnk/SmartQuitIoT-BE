package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Integer> {

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :convId " +
            "AND (:beforeId IS NULL OR m.id < :beforeId) " +
            "ORDER BY m.id DESC")
    List<Message> findMessagesByConversationBeforeId(@Param("convId") int convId,
                                                     @Param("beforeId") Integer beforeId,
                                                     Pageable pageable);

    // tìm tin nhắn trễ nhất
    Optional<Message> findFirstByConversationIdOrderByIdDesc(int convId);

    // đếm những tin nhắn sau lần đọc cuối
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :convId " +
            "AND (:lastReadAt IS NULL OR m.sentAt > :lastReadAt)")
    long countUnread(@Param("convId") int convId, @Param("lastReadAt") LocalDateTime lastReadAt);
}

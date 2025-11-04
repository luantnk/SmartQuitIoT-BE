package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Integer> {
    @Query("SELECT c FROM Conversation c " +
            "JOIN c.participants p1 JOIN c.participants p2 " +
            "WHERE c.type = com.smartquit.smartquitiot.enums.ConversationType.DIRECT " +
            "  AND p1.account.id = :a AND p2.account.id = :b")
    Optional<Conversation> findDirectConversationBetween(@Param("a") int accountA, @Param("b") int accountB);

    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.account.id = :accountId ORDER BY c.lastUpdatedAt DESC")
    List<Conversation> findAllByParticipantAccountId(@Param("accountId") int accountId, Pageable pageable);
}

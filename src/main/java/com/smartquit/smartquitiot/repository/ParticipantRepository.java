package com.smartquit.smartquitiot.repository;

import com.smartquit.smartquitiot.entity.Participant;
import com.smartquit.smartquitiot.entity.Conversation;
import com.smartquit.smartquitiot.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Integer> {
    Optional<Participant> findByConversationAndAccount(Conversation conversation, Account account);

    Optional<Participant> findByConversationIdAndAccountId(int conversationId, int accountId);
}

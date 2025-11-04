package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.MessageCreateRequest;
import com.smartquit.smartquitiot.dto.response.MessageDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.ConversationType;
import com.smartquit.smartquitiot.mapper.MessageMapper;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class    MessageServiceImpl implements MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ParticipantRepository participantRepository;
    private final AccountRepository accountRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public MessageDTO sendMessage(MessageCreateRequest req) {
        Account sender = resolveCurrentAccount();
        if (sender == null) throw new IllegalStateException("Unauthenticated");

        Conversation conv;

        if (req.getConversationId() != null) {
            conv = conversationRepository.findById(req.getConversationId())
                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + req.getConversationId()));

            boolean isParticipant = participantRepository.findByConversationAndAccount(conv, sender).isPresent();
            if (!isParticipant) {
                if (conv.getType() == ConversationType.DIRECT) {
                    Participant p = new Participant();
                    p.setConversation(conv);
                    p.setAccount(sender);
                    participantRepository.save(p);
                    conv.getParticipants().add(p);
                } else {
                    throw new SecurityException("You are not a participant of this group conversation");
                }
            }
        } else if (req.getTargetUserId() != null) {
            if (req.getTargetUserId().equals(sender.getId())) {
                throw new IllegalArgumentException("Cannot send message to yourself");
            }
            Account target = accountRepository.findById(req.getTargetUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

            conv = conversationRepository.findDirectConversationBetween(sender.getId(), target.getId())
                    .orElseGet(() -> {
                        Conversation c = new Conversation();
                        c.setTitle(null);
                        c.setType(ConversationType.DIRECT);
                        c.setLastUpdatedAt(LocalDateTime.now());
                        c = conversationRepository.save(c);

                        Participant p1 = new Participant();
                        p1.setAccount(sender);
                        p1.setConversation(c);
                        participantRepository.save(p1);

                        Participant p2 = new Participant();
                        p2.setAccount(target);
                        p2.setConversation(c);
                        participantRepository.save(p2);

                        c.getParticipants().add(p1);
                        c.getParticipants().add(p2);
                        return c;
                    });
        } else {
            throw new IllegalArgumentException("Either conversationId or targetUserId must be provided");
        }

        // create and save message
        Message msg = new Message();
        msg.setConversation(conv);
        msg.setSender(sender);
        msg.setContent(req.getContent());
        msg.setMessageType(req.getMessageType());
        msg.setDeleted(false);
        // Xử lý tiếp attachment
        if (req.getAttachments() != null && !req.getAttachments().isEmpty()) {
            List<Attachment> attachmentEntities = req.getAttachments().stream()
                    .map(url -> {
                        Attachment att = new Attachment();
                        att.setAttachmentUrl(url);
                        att.setMessage(msg);
                        return att;
                    })
                    .toList();

            msg.setAttachments(attachmentEntities);
        }
        Message saved = messageRepository.save(msg);

        // update conversation lastUpdatedAt
        conv.setLastUpdatedAt(saved.getSentAt());
        conversationRepository.save(conv);

        // ------ FIX: capture finals for inner class ------
        final int convId = conv.getId();
        final Message savedFinal = saved;
        final String clientMessageIdFinal = req.getClientMessageId();

        // broadcast AFTER COMMIT
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        if (messagingTemplate != null) {
                            messagingTemplate.convertAndSend("/topic/conversations/" + convId,
                                    MessageMapper.toResponse(savedFinal, clientMessageIdFinal));
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } else {
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/conversations/" + conv.getId(),
                        MessageMapper.toResponse(saved, req.getClientMessageId()));
            }
        }

        return MessageMapper.toResponse(saved, req.getClientMessageId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageDTO> getMessages(int conversationId, Integer beforeId, int limit) {
        Account current = resolveCurrentAccount();
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        boolean isParticipant = participantRepository.findByConversationAndAccount(conv, current).isPresent();
        if (!isParticipant) {
            throw new SecurityException("Not participant of this conversation");
        }

        List<Message> messages = messageRepository.findMessagesByConversationBeforeId(conversationId, beforeId, PageRequest.of(0, limit));
        return messages.stream().map(m -> MessageMapper.toResponse(m, null)).collect(Collectors.toList());
    }

    private Account resolveCurrentAccount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        String principal = auth.getName();

        Account account = accountRepository.findByUsername(principal).orElse(null);
        if (account == null) {
            try {
                int id = Integer.parseInt(principal);
                account = accountRepository.findById(id).orElse(null);
            } catch (NumberFormatException ignored) {
            }
        }
        return account;
    }
}

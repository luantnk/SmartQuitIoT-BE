package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.MessageCreateRequest;
import com.smartquit.smartquitiot.dto.response.MessageDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.ConversationType;
import com.smartquit.smartquitiot.mapper.MessageMapper;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class); // ADDED: logger

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ParticipantRepository participantRepository;
    private final AccountRepository accountRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AccountService accountService; // <-- inject standardized service

    @Override
    @Transactional
    public MessageDTO sendMessage(MessageCreateRequest req) {
        Account sender = accountService.getAuthenticatedAccount(); // standardized
        if (sender == null) throw new IllegalStateException("Unauthenticated");

        Conversation conv;

        if (req.getConversationId() != null) {
            conv = conversationRepository.findById(req.getConversationId())
                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + req.getConversationId()));

            boolean isParticipant = participantRepository.findByConversationAndAccount(conv, sender).isPresent();
            if (!isParticipant) {
                if (conv.getType() == ConversationType.DIRECT) {
                    // avoid duplicates: check again by account id
                    boolean already = conv.getParticipants() == null
                            ? false
                            : conv.getParticipants().stream()
                            .anyMatch(p -> p.getAccount() != null && p.getAccount().getId() == sender.getId());

                    if (!already) {
                        Participant p = new Participant();
                        p.setConversation(conv);
                        p.setAccount(sender);
                        participantRepository.save(p);
                        // ensure list initialized
                        if (conv.getParticipants() == null) conv.setParticipants(new java.util.ArrayList<>());
                        conv.getParticipants().add(p);
                    }
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

            Optional<Conversation> convOpt = conversationRepository.findDirectConversationBetween(sender.getId(), target.getId());
            if (convOpt.isPresent()) {
                conv = convOpt.get();
            } else {
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

                c.setParticipants(new java.util.ArrayList<>());
                c.getParticipants().add(p1);
                c.getParticipants().add(p2);
                conv = c;
            }
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

        if (req.getAttachments() != null && !req.getAttachments().isEmpty()) {
            List<Attachment> attachmentEntities = req.getAttachments().stream()
                    .map(url -> {
                        Attachment att = new Attachment();
                        att.setAttachmentUrl(url);
                        att.setMessage(msg);
                        return att;
                    })
                    .collect(Collectors.toList());
            msg.setAttachments(attachmentEntities);
        }

        Message saved = messageRepository.save(msg);

        // update conversation lastUpdatedAt
        conv.setLastUpdatedAt(saved.getSentAt() != null ? saved.getSentAt() : LocalDateTime.now());
        conversationRepository.save(conv);

        final int convId = conv.getId();
        final Message savedFinal = saved;
        final String clientMessageIdFinal = req.getClientMessageId();

        // ADDED: debug log before register synchronization
        log.info("[MSG] prepared to broadcast after commit convId={} msgId={} clientMessageId={}",
                convId, savedFinal.getId(), clientMessageIdFinal);

        // broadcast AFTER COMMIT
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        MessageDTO payload = MessageMapper.toResponse(savedFinal, clientMessageIdFinal);
                        log.info("[MSG] afterCommit broadcasting to /topic/conversations/{} payload={}", convId, payload); // ADDED
                        messagingTemplate.convertAndSend("/topic/conversations/" + convId, payload);
                        log.info("[MSG] broadcast done convId={} clientMessageId={}", convId, clientMessageIdFinal); // ADDED
                    } catch (Exception ex) {
                        // ADDED: proper error log
                        log.error("[MSG] broadcast error convId={} clientMessageId={}", convId, clientMessageIdFinal, ex);
                    }
                }
            });
        } else {
            try {
                MessageDTO payload = MessageMapper.toResponse(saved, req.getClientMessageId());
                log.info("[MSG] no-tx broadcasting to /topic/conversations/{} payload={}", conv.getId(), payload); // ADDED
                messagingTemplate.convertAndSend("/topic/conversations/" + conv.getId(), payload);
            } catch (Exception ex) {
                log.error("[MSG] no-tx broadcast error convId={}", conv.getId(), ex); // ADDED
            }
        }

        return MessageMapper.toResponse(saved, req.getClientMessageId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageDTO> getMessages(int conversationId, Integer beforeId, int limit) {
        Account current = accountService.getAuthenticatedAccount();
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        boolean isParticipant = participantRepository.findByConversationAndAccount(conv, current).isPresent();
        if (!isParticipant) {
            throw new SecurityException("Not participant of this conversation");
        }

        List<Message> messages = messageRepository.findMessagesByConversationBeforeId(conversationId, beforeId, PageRequest.of(0, limit));
        return messages.stream().map(m -> MessageMapper.toResponse(m, null)).collect(Collectors.toList());
    }
}

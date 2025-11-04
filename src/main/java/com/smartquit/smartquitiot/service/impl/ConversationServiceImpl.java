package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.*;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Conversation;
import com.smartquit.smartquitiot.entity.Message;
import com.smartquit.smartquitiot.entity.Participant;
import com.smartquit.smartquitiot.repository.AccountRepository;
import com.smartquit.smartquitiot.repository.ConversationRepository;
import com.smartquit.smartquitiot.repository.MessageRepository;
import com.smartquit.smartquitiot.repository.ParticipantRepository;
import com.smartquit.smartquitiot.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ParticipantRepository participantRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ConversationSummaryDTO> listConversationsForCurrentUser(int page, int size) {
        Account current = resolveCurrentAccount();
        if (current == null) throw new IllegalStateException("Unauthenticated");

        List<Conversation> convs = conversationRepository.findAllByParticipantAccountId(current.getId(), PageRequest.of(page, size));

        return convs.stream().map(conv -> {
            // last message
            LastMessageDTO lastMsgDto = messageRepository
                    .findFirstByConversationIdOrderByIdDesc(conv.getId())
                    .map(this::mapToLastMessageDTO)
                    .orElse(null);

            //  lastReadAt
            var participantOpt = participantRepository.findByConversationIdAndAccountId(conv.getId(), current.getId());
            LocalDateTime lastReadAt = participantOpt.map(Participant::getLastReadAt).orElse(null);

            long unread = messageRepository.countUnread(conv.getId(), lastReadAt);

            // participants summary
            List<ParticipantSummaryDTO> parts = conv.getParticipants().stream()
                    .map(p -> {
                        var acc = p.getAccount();
                        if (acc == null) return new ParticipantSummaryDTO(0, null, null);

                        String fullName = null;
                        String avatar = null;

                        var member = acc.getMember();
                        if (member != null) {
                            String fn = member.getFirstName() == null ? "" : member.getFirstName().trim();
                            String ln = member.getLastName() == null ? "" : member.getLastName().trim();
                            fullName = (fn + " " + ln).trim();
                            avatar = member.getAvatarUrl();
                        }

                        if ((fullName == null || fullName.isEmpty()) && acc.getCoach() != null) {
                            var coach = acc.getCoach();
                            String fn = coach.getFirstName() == null ? "" : coach.getFirstName().trim();
                            String ln = coach.getLastName() == null ? "" : coach.getLastName().trim();
                            fullName = (fn + " " + ln).trim();
                            if (avatar == null) avatar = coach.getAvatarUrl();
                        }

                        if (fullName == null || fullName.isEmpty()) {
                            fullName = acc.getUsername();
                        }

                        return new ParticipantSummaryDTO(acc.getId(), fullName, avatar);
                    }).collect(Collectors.toList());

            return ConversationSummaryDTO.builder()
                    .conversationId(conv.getId())
                    .title(conv.getTitle())
                    .type(conv.getType())
                    .lastUpdatedAt(conv.getLastUpdatedAt())
                    .lastMessage(lastMsgDto)
                    .unreadCount(unread)
                    .participants(parts)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConversationSummaryDTO markConversationRead(int conversationId) {
        Account current = resolveCurrentAccount();
        if (current == null) throw new IllegalStateException("Unauthenticated");

        Participant participant = participantRepository.findByConversationIdAndAccountId(conversationId, current.getId())
                .orElseThrow(() -> new IllegalArgumentException("You are not a participant of this conversation"));

        participant.setLastReadAt(LocalDateTime.now());
        participantRepository.save(participant);

        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // last message
        LastMessageDTO lastMsgDto = messageRepository
                .findFirstByConversationIdOrderByIdDesc(conv.getId())
                .map(this::mapToLastMessageDTO)
                .orElse(null);

        List<ParticipantSummaryDTO> parts = conv.getParticipants().stream()
                .map(p -> {
                    var acc = p.getAccount();
                    if (acc == null) return new ParticipantSummaryDTO(0, null, null);

                    String fullName = null;
                    String avatar = null;

                    var member = acc.getMember();
                    if (member != null) {
                        String fn = member.getFirstName() == null ? "" : member.getFirstName().trim();
                        String ln = member.getLastName() == null ? "" : member.getLastName().trim();
                        fullName = (fn + " " + ln).trim();
                        avatar = member.getAvatarUrl();
                    }

                    if ((fullName == null || fullName.isEmpty()) && acc.getCoach() != null) {
                        var coach = acc.getCoach();
                        String fn = coach.getFirstName() == null ? "" : coach.getFirstName().trim();
                        String ln = coach.getLastName() == null ? "" : coach.getLastName().trim();
                        fullName = (fn + " " + ln).trim();
                        if (avatar == null) avatar = coach.getAvatarUrl();
                    }

                    if (fullName == null || fullName.isEmpty()) {
                        fullName = acc.getUsername();
                    }

                    return new ParticipantSummaryDTO(acc.getId(), fullName, avatar);
                }).collect(Collectors.toList());

        long unread = messageRepository.countUnread(conv.getId(), participant.getLastReadAt());

        return ConversationSummaryDTO.builder()
                .conversationId(conv.getId())
                .title(conv.getTitle())
                .type(conv.getType())
                .lastUpdatedAt(conv.getLastUpdatedAt())
                .lastMessage(lastMsgDto)
                .unreadCount(unread)
                .participants(parts)
                .build();
    }

    private LastMessageDTO mapToLastMessageDTO(Message m) {
        if (m == null) return null;
        return new LastMessageDTO(m.getId(), m.getSender() != null ? m.getSender().getId() : 0, m.getMessageType(), m.getContent(), m.getSentAt());
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

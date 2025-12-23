package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.ConversationSummaryDTO;
import com.smartquit.smartquitiot.dto.response.LastMessageDTO;
import com.smartquit.smartquitiot.dto.response.ParticipantSummaryDTO;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Conversation;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.entity.Message;
import com.smartquit.smartquitiot.entity.Participant;
import com.smartquit.smartquitiot.enums.ConversationType;
import com.smartquit.smartquitiot.enums.MessageType;
import com.smartquit.smartquitiot.repository.AccountRepository;
import com.smartquit.smartquitiot.repository.ConversationRepository;
import com.smartquit.smartquitiot.repository.MessageRepository;
import com.smartquit.smartquitiot.repository.ParticipantRepository;
import com.smartquit.smartquitiot.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    // Test data
    private Account currentAccount;
    private Account otherAccount;
    private Conversation conversation;
    private Participant participant;
    private Message lastMessage;
    private Member member;

    @BeforeEach
    void setUp() {
        // Setup current account
        currentAccount = new Account();
        currentAccount.setId(100);
        currentAccount.setUsername("currentuser");
        currentAccount.setEmail("current@example.com");

        // Setup other account
        otherAccount = new Account();
        otherAccount.setId(200);
        otherAccount.setUsername("otheruser");
        otherAccount.setEmail("other@example.com");

        // Setup member
        member = new Member();
        member.setId(1);
        member.setFirstName("John");
        member.setLastName("Doe");
        member.setAvatarUrl("http://avatar.url");
        member.setAccount(otherAccount);
        otherAccount.setMember(member);

        // Setup conversation
        conversation = new Conversation();
        conversation.setId(1);
        conversation.setTitle("Test Conversation");
        conversation.setType(ConversationType.DIRECT);
        conversation.setLastUpdatedAt(LocalDateTime.now());
        conversation.setParticipants(new ArrayList<>());

        // Setup participant
        participant = new Participant();
        participant.setId(1);
        participant.setAccount(currentAccount);
        participant.setConversation(conversation);
        participant.setLastReadAt(LocalDateTime.now().minusHours(1));

        Participant otherParticipant = new Participant();
        otherParticipant.setId(2);
        otherParticipant.setAccount(otherAccount);
        otherParticipant.setConversation(conversation);

        conversation.getParticipants().add(participant);
        conversation.getParticipants().add(otherParticipant);

        // Setup last message
        lastMessage = new Message();
        lastMessage.setId(10);
        lastMessage.setContent("Last message content");
        lastMessage.setMessageType(MessageType.TEXT);
        lastMessage.setSender(otherAccount);
        lastMessage.setSentAt(LocalDateTime.now());
        lastMessage.setConversation(conversation);
    }

    // ========== listConversationsForCurrentUser Tests ==========

    @Test
    void should_list_conversations_for_current_user_successfully() {
        // ===== GIVEN =====
        int page = 0;
        int size = 10;
        List<Conversation> conversations = List.of(conversation);

        when(accountService.getAuthenticatedAccount()).thenReturn(currentAccount);
        when(conversationRepository.findAllByParticipantAccountId(
                currentAccount.getId(), PageRequest.of(page, size)))
                .thenReturn(conversations);
        when(messageRepository.findFirstByConversationIdOrderByIdDesc(conversation.getId()))
                .thenReturn(Optional.of(lastMessage));
        when(participantRepository.findByConversationIdAndAccountId(conversation.getId(), currentAccount.getId()))
                .thenReturn(Optional.of(participant));
        when(messageRepository.countUnread(conversation.getId(), participant.getLastReadAt()))
                .thenReturn(5L);

        // ===== WHEN =====
        List<ConversationSummaryDTO> result = conversationService.listConversationsForCurrentUser(page, size);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);

        ConversationSummaryDTO dto = result.get(0);
        assertThat(dto.getConversationId()).isEqualTo(conversation.getId());
        assertThat(dto.getTitle()).isEqualTo(conversation.getTitle());
        assertThat(dto.getType()).isEqualTo(conversation.getType());
        assertThat(dto.getUnreadCount()).isEqualTo(5L);
        assertThat(dto.getParticipants()).isNotNull();
        assertThat(dto.getParticipants().size()).isEqualTo(2);
        assertThat(dto.getLastMessage()).isNotNull();
        assertThat(dto.getLastMessage().getContent()).isEqualTo(lastMessage.getContent());
    }

    @Test
    void should_return_empty_list_when_no_conversations() {
        // ===== GIVEN =====
        int page = 0;
        int size = 10;

        when(accountService.getAuthenticatedAccount()).thenReturn(currentAccount);
        when(conversationRepository.findAllByParticipantAccountId(
                currentAccount.getId(), PageRequest.of(page, size)))
                .thenReturn(List.of());

        // ===== WHEN =====
        List<ConversationSummaryDTO> result = conversationService.listConversationsForCurrentUser(page, size);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void should_handle_conversation_without_last_message() {
        // ===== GIVEN =====
        int page = 0;
        int size = 10;
        List<Conversation> conversations = List.of(conversation);

        when(accountService.getAuthenticatedAccount()).thenReturn(currentAccount);
        when(conversationRepository.findAllByParticipantAccountId(
                currentAccount.getId(), PageRequest.of(page, size)))
                .thenReturn(conversations);
        when(messageRepository.findFirstByConversationIdOrderByIdDesc(conversation.getId()))
                .thenReturn(Optional.empty());
        when(participantRepository.findByConversationIdAndAccountId(conversation.getId(), currentAccount.getId()))
                .thenReturn(Optional.of(participant));
        when(messageRepository.countUnread(conversation.getId(), participant.getLastReadAt()))
                .thenReturn(0L);

        // ===== WHEN =====
        List<ConversationSummaryDTO> result = conversationService.listConversationsForCurrentUser(page, size);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getLastMessage()).isNull();
        assertThat(result.get(0).getUnreadCount()).isEqualTo(0L);
    }

    @Test
    void should_handle_conversation_without_participant_record() {
        // ===== GIVEN =====
        int page = 0;
        int size = 10;
        List<Conversation> conversations = List.of(conversation);

        when(accountService.getAuthenticatedAccount()).thenReturn(currentAccount);
        when(conversationRepository.findAllByParticipantAccountId(
                currentAccount.getId(), PageRequest.of(page, size)))
                .thenReturn(conversations);
        when(messageRepository.findFirstByConversationIdOrderByIdDesc(conversation.getId()))
                .thenReturn(Optional.of(lastMessage));
        when(participantRepository.findByConversationIdAndAccountId(conversation.getId(), currentAccount.getId()))
                .thenReturn(Optional.empty());
        when(messageRepository.countUnread(conversation.getId(), null))
                .thenReturn(10L);

        // ===== WHEN =====
        List<ConversationSummaryDTO> result = conversationService.listConversationsForCurrentUser(page, size);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getUnreadCount()).isEqualTo(10L);
    }

    @Test
    void should_use_username_when_participant_has_no_name() {
        // ===== GIVEN =====
        int page = 0;
        int size = 10;

        // Create account without member or coach
        Account accountWithoutName = new Account();
        accountWithoutName.setId(300);
        accountWithoutName.setUsername("usernameonly");

        Participant participantWithoutName = new Participant();
        participantWithoutName.setAccount(accountWithoutName);

        Conversation conv = new Conversation();
        conv.setId(2);
        conv.setParticipants(List.of(participantWithoutName));

        when(accountService.getAuthenticatedAccount()).thenReturn(currentAccount);
        when(conversationRepository.findAllByParticipantAccountId(
                currentAccount.getId(), PageRequest.of(page, size)))
                .thenReturn(List.of(conv));
        when(messageRepository.findFirstByConversationIdOrderByIdDesc(conv.getId()))
                .thenReturn(Optional.empty());
        when(participantRepository.findByConversationIdAndAccountId(conv.getId(), currentAccount.getId()))
                .thenReturn(Optional.of(participant));
        when(messageRepository.countUnread(anyInt(), any())).thenReturn(0L);

        // ===== WHEN =====
        List<ConversationSummaryDTO> result = conversationService.listConversationsForCurrentUser(page, size);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        List<ParticipantSummaryDTO> participants = result.get(0).getParticipants();
        assertThat(participants).isNotNull();
        ParticipantSummaryDTO participantDTO = participants.stream()
                .filter(p -> p.getId() == accountWithoutName.getId())
                .findFirst()
                .orElse(null);
        assertThat(participantDTO).isNotNull();
        assertThat(participantDTO.getFullName()).isEqualTo("usernameonly");
    }

    @Test
    void should_throw_exception_when_unauthenticated() {
        // ===== GIVEN =====
        when(accountService.getAuthenticatedAccount()).thenReturn(null);

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> conversationService.listConversationsForCurrentUser(0, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unauthenticated");
    }

    // ========== markConversationRead Tests ==========

    @Test
    void should_mark_conversation_read_successfully() {
        // ===== GIVEN =====
        int conversationId = 1;
        LocalDateTime beforeUpdate = participant.getLastReadAt();

        when(accountService.getAuthenticatedAccount()).thenReturn(currentAccount);
        when(participantRepository.findByConversationIdAndAccountId(conversationId, currentAccount.getId()))
                .thenReturn(Optional.of(participant));
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findFirstByConversationIdOrderByIdDesc(conversationId))
                .thenReturn(Optional.of(lastMessage));
        when(messageRepository.countUnread(eq(conversationId), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ===== WHEN =====
        ConversationSummaryDTO result = conversationService.markConversationRead(conversationId);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getConversationId()).isEqualTo(conversationId);
        assertThat(result.getUnreadCount()).isEqualTo(0L);

        // Verify lastReadAt was updated
        assertThat(participant.getLastReadAt()).isNotNull();
        assertThat(participant.getLastReadAt()).isAfter(beforeUpdate);
    }

    @Test
    void should_throw_exception_when_user_not_participant() {
        // ===== GIVEN =====
        int conversationId = 1;

        when(accountService.getAuthenticatedAccount()).thenReturn(currentAccount);
        when(participantRepository.findByConversationIdAndAccountId(conversationId, currentAccount.getId()))
                .thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> conversationService.markConversationRead(conversationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a participant");

        verify(participantRepository, never()).save(any());
        verify(conversationRepository, never()).findById(anyInt());
    }

    @Test
    void should_throw_exception_when_conversation_not_found() {
        // ===== GIVEN =====
        int conversationId = 999;

        when(accountService.getAuthenticatedAccount()).thenReturn(currentAccount);
        when(participantRepository.findByConversationIdAndAccountId(conversationId, currentAccount.getId()))
                .thenReturn(Optional.of(participant));
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> conversationService.markConversationRead(conversationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conversation not found");
    }

    @Test
    void should_throw_exception_when_unauthenticated_for_mark_read() {
        // ===== GIVEN =====
        when(accountService.getAuthenticatedAccount()).thenReturn(null);

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> conversationService.markConversationRead(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unauthenticated");

        verify(participantRepository, never()).save(any());
    }

    @Test
    void should_include_all_participants_in_summary() {
        // ===== GIVEN =====
        int conversationId = 1;

        when(accountService.getAuthenticatedAccount()).thenReturn(currentAccount);
        when(participantRepository.findByConversationIdAndAccountId(conversationId, currentAccount.getId()))
                .thenReturn(Optional.of(participant));
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findFirstByConversationIdOrderByIdDesc(conversationId))
                .thenReturn(Optional.of(lastMessage));
        when(messageRepository.countUnread(eq(conversationId), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ===== WHEN =====
        ConversationSummaryDTO result = conversationService.markConversationRead(conversationId);

        // ===== THEN =====
        assertThat(result.getParticipants()).isNotNull();
        assertThat(result.getParticipants().size()).isEqualTo(2);

        // Verify participant with member info is included
        ParticipantSummaryDTO memberParticipant = result.getParticipants().stream()
                .filter(p -> p.getId() == otherAccount.getId())
                .findFirst()
                .orElse(null);
        assertThat(memberParticipant).isNotNull();
        assertThat(memberParticipant.getFullName()).isEqualTo("John Doe");
        assertThat(memberParticipant.getAvatarUrl()).isEqualTo("http://avatar.url");
    }
}
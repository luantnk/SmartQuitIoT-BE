package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.MessageCreateRequest;
import com.smartquit.smartquitiot.dto.response.MessageDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.ConversationType;
import com.smartquit.smartquitiot.enums.MessageType;
import com.smartquit.smartquitiot.mapper.MessageMapper;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private MessageServiceImpl messageService;

    // Test data
    private Account senderAccount;
    private Account targetAccount;
    private Member targetMember;
    private Conversation conversation;
    private Participant senderParticipant;
    private Participant targetParticipant;
    private Message message;
    private MessageCreateRequest request;

    @BeforeEach
    void setUp() {
        // Setup sender account
        senderAccount = new Account();
        senderAccount.setId(100);
        senderAccount.setUsername("sender");
        senderAccount.setEmail("sender@example.com");

        // Setup target account
        targetAccount = new Account();
        targetAccount.setId(200);
        targetAccount.setUsername("target");
        targetAccount.setEmail("target@example.com");

        // Setup target member
        targetMember = new Member();
        targetMember.setId(1);
        targetMember.setAccount(targetAccount);
        targetAccount.setMember(targetMember);

        // Setup conversation
        conversation = new Conversation();
        conversation.setId(1);
        conversation.setType(ConversationType.DIRECT);
        conversation.setLastUpdatedAt(LocalDateTime.now());
        conversation.setParticipants(new ArrayList<>());

        // Setup participants
        senderParticipant = new Participant();
        senderParticipant.setId(1);
        senderParticipant.setAccount(senderAccount);
        senderParticipant.setConversation(conversation);

        targetParticipant = new Participant();
        targetParticipant.setId(2);
        targetParticipant.setAccount(targetAccount);
        targetParticipant.setConversation(conversation);

        conversation.getParticipants().add(senderParticipant);
        conversation.getParticipants().add(targetParticipant);

        // Setup message
        message = new Message();
        message.setId(10);
        message.setContent("Test message");
        message.setMessageType(MessageType.TEXT);
        message.setSender(senderAccount);
        message.setConversation(conversation);
        message.setSentAt(LocalDateTime.now());
        message.setDeleted(false);
        message.setAttachments(new ArrayList<>());

        // Setup request
        request = new MessageCreateRequest();
        request.setContent("Test message");
        request.setMessageType(MessageType.TEXT);
        request.setClientMessageId("client-msg-123");
    }


    @Test
    void should_throw_exception_when_unauthenticated() {
        // ===== GIVEN =====
        request.setConversationId(1);

        when(accountService.getAuthenticatedAccount()).thenReturn(null);

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unauthenticated");
    }

    @Test
    void should_throw_exception_when_no_conversation_or_target_provided() {
        // ===== GIVEN =====
        request.setConversationId(null);
        request.setTargetMemberId(null);
        request.setTargetUserId(null);

        when(accountService.getAuthenticatedAccount()).thenReturn(senderAccount);

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either conversationId or (targetMemberId or targetUserId) must be provided");
    }

    @Test
    void should_throw_exception_when_conversation_not_found() {
        // ===== GIVEN =====
        request.setConversationId(999);

        when(accountService.getAuthenticatedAccount()).thenReturn(senderAccount);
        when(conversationRepository.findById(999)).thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conversation not found");
    }

    @Test
    void should_add_participant_when_sender_not_in_direct_conversation() {
        // ===== GIVEN =====
        request.setConversationId(1);
        Conversation directConv = new Conversation();
        directConv.setId(1);
        directConv.setType(ConversationType.DIRECT);
        directConv.setParticipants(new ArrayList<>());
        directConv.getParticipants().add(targetParticipant);

        when(accountService.getAuthenticatedAccount()).thenReturn(senderAccount);
        when(conversationRepository.findById(1)).thenReturn(Optional.of(directConv));
        when(participantRepository.findByConversationAndAccount(directConv, senderAccount))
                .thenReturn(Optional.empty());
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> {
            Participant p = invocation.getArgument(0);
            p.setId(3);
            return p;
        });
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(10);
            msg.setSentAt(LocalDateTime.now());
            return msg;
        });
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ===== WHEN =====
        MessageDTO result = messageService.sendMessage(request);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(directConv.getParticipants().size()).isEqualTo(2); // Added sender participant
    }

    @Test
    void should_throw_exception_when_not_participant_of_group_conversation() {
        // ===== GIVEN =====
        request.setConversationId(1);
        Conversation groupConv = new Conversation();
        groupConv.setId(1);
        groupConv.setType(ConversationType.GROUP);
        groupConv.setParticipants(new ArrayList<>());
        groupConv.getParticipants().add(targetParticipant);

        when(accountService.getAuthenticatedAccount()).thenReturn(senderAccount);
        when(conversationRepository.findById(1)).thenReturn(Optional.of(groupConv));
        when(participantRepository.findByConversationAndAccount(groupConv, senderAccount))
                .thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("not a participant of this group conversation");
    }

    // ========== sendMessage Tests - With targetMemberId ==========


    @Test
    void should_throw_exception_when_target_member_not_found() {
        // ===== GIVEN =====
        request.setConversationId(null);
        request.setTargetMemberId(999);

        when(accountService.getAuthenticatedAccount()).thenReturn(senderAccount);
        when(memberRepository.findById(999)).thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target member not found");
    }

    @Test
    void should_throw_exception_when_target_member_has_no_account() {
        // ===== GIVEN =====
        request.setConversationId(null);
        request.setTargetMemberId(1);

        Member memberWithoutAccount = new Member();
        memberWithoutAccount.setId(1);
        memberWithoutAccount.setAccount(null);

        when(accountService.getAuthenticatedAccount()).thenReturn(senderAccount);
        when(memberRepository.findById(1)).thenReturn(Optional.of(memberWithoutAccount));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target member has no account linked");
    }

    // ========== sendMessage Tests - With targetUserId ==========

    @Test
    void should_send_message_when_using_target_user_id() {
        // ===== GIVEN =====
        request.setConversationId(null);
        request.setTargetMemberId(null);
        request.setTargetUserId(200);

        when(accountService.getAuthenticatedAccount()).thenReturn(senderAccount);
        when(accountRepository.findById(200)).thenReturn(Optional.of(targetAccount));
        when(conversationRepository.findDirectConversationBetween(senderAccount.getId(), 200))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(10);
            msg.setSentAt(LocalDateTime.now());
            return msg;
        });
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ===== WHEN =====
        MessageDTO result = messageService.sendMessage(request);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Test message");
    }

    @Test
    void should_throw_exception_when_sending_to_oneself() {
        // ===== GIVEN =====
        request.setConversationId(null);
        request.setTargetUserId(senderAccount.getId()); // Same as sender

        when(accountService.getAuthenticatedAccount()).thenReturn(senderAccount);

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot send message to yourself");
    }

    @Test
    void should_throw_exception_when_target_account_not_found() {
        // ===== GIVEN =====
        request.setConversationId(null);
        request.setTargetUserId(999);

        when(accountService.getAuthenticatedAccount()).thenReturn(senderAccount);
        when(accountRepository.findById(999)).thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> messageService.sendMessage(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target account not found");
    }

    // ========== getMessages Tests ==========

    @Test
    void should_get_messages_successfully() {
        // ===== GIVEN =====
        int conversationId = 1;
        Integer beforeId = null;
        int limit = 10;

        List<Message> messages = List.of(message);

        when(accountService.getAuthenticatedAccount()).thenReturn(senderAccount);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(participantRepository.findByConversationAndAccount(conversation, senderAccount))
                .thenReturn(Optional.of(senderParticipant));
        when(messageRepository.findMessagesByConversationBeforeId(
                conversationId, beforeId, PageRequest.of(0, limit)))
                .thenReturn(messages);

        // ===== WHEN =====
        List<MessageDTO> result = messageService.getMessages(conversationId, beforeId, limit);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getContent()).isEqualTo("Test message");
    }

    @Test
    void should_get_messages_with_before_id() {
        // ===== GIVEN =====
        int conversationId = 1;
        Integer beforeId = 20;
        int limit = 10;

        List<Message> messages = List.of(message);

        when(accountService.getAuthenticatedAccount()).thenReturn(senderAccount);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(participantRepository.findByConversationAndAccount(conversation, senderAccount))
                .thenReturn(Optional.of(senderParticipant));
        when(messageRepository.findMessagesByConversationBeforeId(
                conversationId, beforeId, PageRequest.of(0, limit)))
                .thenReturn(messages);

        // ===== WHEN =====
        List<MessageDTO> result = messageService.getMessages(conversationId, beforeId, limit);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    void should_throw_exception_when_conversation_not_found_for_get_messages() {
        // ===== GIVEN =====
        int conversationId = 999;
        Integer beforeId = null;
        int limit = 10;

        when(accountService.getAuthenticatedAccount()).thenReturn(senderAccount);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> messageService.getMessages(conversationId, beforeId, limit))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conversation not found");
    }

    @Test
    void should_throw_exception_when_not_participant_for_get_messages() {
        // ===== GIVEN =====
        int conversationId = 1;
        Integer beforeId = null;
        int limit = 10;

        when(accountService.getAuthenticatedAccount()).thenReturn(senderAccount);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(participantRepository.findByConversationAndAccount(conversation, senderAccount))
                .thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> messageService.getMessages(conversationId, beforeId, limit))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Not participant of this conversation");
    }

    @Test
    void should_return_empty_list_when_no_messages() {
        // ===== GIVEN =====
        int conversationId = 1;
        Integer beforeId = null;
        int limit = 10;

        when(accountService.getAuthenticatedAccount()).thenReturn(senderAccount);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(participantRepository.findByConversationAndAccount(conversation, senderAccount))
                .thenReturn(Optional.of(senderParticipant));
        when(messageRepository.findMessagesByConversationBeforeId(
                conversationId, beforeId, PageRequest.of(0, limit)))
                .thenReturn(List.of());

        // ===== WHEN =====
        List<MessageDTO> result = messageService.getMessages(conversationId, beforeId, limit);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isTrue();
    }
}
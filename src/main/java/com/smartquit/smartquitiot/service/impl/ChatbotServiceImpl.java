package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.ChatbotPayload;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.service.ChatbotService;
import com.smartquit.smartquitiot.service.MemberService;
import com.smartquit.smartquitiot.toolcalling.ChatbotTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.smartquit.smartquitiot.toolcalling.ChatbotTools.CHATBOT_PROMPT;

@Service
public class ChatbotServiceImpl implements ChatbotService {

    private final JdbcChatMemoryRepository chatMemoryRepository;
    private final MemberService memberService;
    private final ChatClient chatClient;
    private final ChatbotTools chatbotTools;

    public ChatbotServiceImpl(ChatClient.Builder chatClientBuilder,
                              JdbcChatMemoryRepository chatMemoryRepository,
                              MemberService memberService,
                              ChatbotTools chatbotTools) {
        this.chatMemoryRepository = chatMemoryRepository;
        this.memberService = memberService;
        ChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(30)
                .build();

        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.chatbotTools = chatbotTools;

    }

    @Override
    public List<Message> getChatbotMessagesByConversationId(Integer conversationId) {
        String strConversationId = String.valueOf(conversationId);
        return chatMemoryRepository.findByConversationId(strConversationId);
    }


    @Override
    public AssistantMessage personalizedChat(ChatbotPayload payload) {
        MemberDTO member = memberService.getMemberById(Integer.parseInt(payload.getMemberId()));

        SystemMessage systemMessage = new SystemMessage(CHATBOT_PROMPT + "with MemberId: " + member.getId());
        UserMessage userMessage = new UserMessage(payload.getMessage());
        Prompt prompt = new Prompt(systemMessage, userMessage);

        return chatClient.prompt(prompt)
                .tools(chatbotTools)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, member.getId()))
                .call().chatClientResponse().chatResponse().getResult().getOutput();

    }
}

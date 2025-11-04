package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.response.ConversationSummaryDTO;

import java.util.List;

public interface ConversationService {

    List<ConversationSummaryDTO> listConversationsForCurrentUser(int page, int size);
    ConversationSummaryDTO markConversationRead(int conversationId);
}

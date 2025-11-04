package com.smartquit.smartquitiot.dto.response;

import com.smartquit.smartquitiot.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryDTO {
    private int conversationId;
    private String title;
    private ConversationType type;
    private LocalDateTime lastUpdatedAt;
    private LastMessageDTO lastMessage;
    private long unreadCount;
    private List<ParticipantSummaryDTO> participants;
}

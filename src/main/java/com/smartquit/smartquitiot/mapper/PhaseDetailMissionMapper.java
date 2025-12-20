package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.ChatbotMissionResponse;
import com.smartquit.smartquitiot.entity.PhaseDetailMission;
import org.springframework.stereotype.Component;

@Component
public class PhaseDetailMissionMapper {

    public ChatbotMissionResponse toChatbotMissionResponse(PhaseDetailMission phaseDetailMission) {
        ChatbotMissionResponse response = new ChatbotMissionResponse();
        response.setMissionName(phaseDetailMission.getName());
        response.setMissionDescription(phaseDetailMission.getDescription());
        response.setMissionStatus(phaseDetailMission.getStatus().name());
        return response;

    }
}

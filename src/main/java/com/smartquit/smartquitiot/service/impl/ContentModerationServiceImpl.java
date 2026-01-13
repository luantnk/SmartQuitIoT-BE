package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.client.AiServiceClient;
import com.smartquit.smartquitiot.dto.response.ContentCheckResponseDTO;
import com.smartquit.smartquitiot.service.ContentModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentModerationServiceImpl implements ContentModerationService {

    private final AiServiceClient aiServiceClient;

    @Override
    public boolean isTextToxic(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        return callAiSafely(() -> aiServiceClient.checkText(Map.of("text", text)));
    }

    @Override
    public boolean isImageNsfw(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return false;
        return callAiSafely(() -> aiServiceClient.checkImage(Map.of("url", imageUrl)));
    }

    @Override
    public boolean isVideoNsfw(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) return false;
        return callAiSafely(() -> aiServiceClient.checkVideo(Map.of("url", videoUrl)));
    }

    private boolean callAiSafely(java.util.function.Supplier<ContentCheckResponseDTO> aiCall) {
        try {
            ContentCheckResponseDTO response = aiCall.get();
            return response != null && response.isToxic();
        } catch (Exception e) {
            log.error("AI Service Error: {}", e.getMessage());
            throw new RuntimeException("AI Service Unavailable or Error: " + e.getMessage());
        }
    }
}
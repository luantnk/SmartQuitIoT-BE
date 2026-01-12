package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.ContentCheckResponseDTO;
import com.smartquit.smartquitiot.service.ContentModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentModerationServiceImpl implements ContentModerationService {

    private final RestTemplate restTemplate;

    @Value("${AI_SERVICE_URL:${app.ai.service.url:http://127.0.0.1:8000}}")
    private String aiServiceUrl;

    @Override
    public boolean isTextToxic(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        return callAiService("/check-content", "text", text);
    }

    @Override
    public boolean isImageNsfw(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return false;
        return callAiService("/check-image-url", "url", imageUrl);
    }

    @Override
    public boolean isVideoNsfw(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) return false;
        return callAiService("/check-video-url", "url", videoUrl);
    }

    private boolean callAiService(String endpoint, String jsonKey, String value) {
        String apiUrl = aiServiceUrl + endpoint;
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put(jsonKey, value);
            ContentCheckResponseDTO response = restTemplate.postForObject(apiUrl, requestBody, ContentCheckResponseDTO.class);
            return response != null && response.isToxic();

        } catch (Exception e) {
            log.error("AI Service Error at {}: {}", endpoint, e.getMessage());
            throw new RuntimeException("AI Service Unavailable or Error: " + e.getMessage());
        }
    }
}
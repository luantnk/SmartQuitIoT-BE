package com.smartquit.smartquitiot.client;

import com.smartquit.smartquitiot.dto.request.AiPredictionRequest;
import com.smartquit.smartquitiot.dto.response.AiPredictionResponse;
import com.smartquit.smartquitiot.dto.response.ContentCheckResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "ai-service", url = "${app.ai.service.url}")
public interface AiServiceClient {

    @PostMapping("/check-content")
    ContentCheckResponseDTO checkText(@RequestBody Map<String, String> requestBody);

    @PostMapping("/check-image-url")
    ContentCheckResponseDTO checkImage(@RequestBody Map<String, String> requestBody);

    @PostMapping("/check-video-url")
    ContentCheckResponseDTO checkVideo(@RequestBody Map<String, String> requestBody);

    @PostMapping("/predict-quit-status")
    AiPredictionResponse predictQuitStatus(@RequestBody AiPredictionRequest request);
}
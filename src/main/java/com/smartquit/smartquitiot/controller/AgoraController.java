package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.service.AgoraService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agora")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AgoraController {

    private final AgoraService agoraService;

    @GetMapping("/token")
    public GlobalResponse<String> generateToken(
            @RequestParam String channelName,
            @RequestParam int uid,
            @RequestParam(required = false) Integer ttlSeconds // optional
    ) {
        try {
            int ttl = (ttlSeconds != null) ? ttlSeconds : 3600;
            String token = agoraService.generateRtcToken(channelName, uid, ttl);
            return GlobalResponse.ok("Create token successfully", token);
        } catch (Exception e) {
            return GlobalResponse.error("Error when creating token: " + e.getMessage(), 500);
        }
    }
}


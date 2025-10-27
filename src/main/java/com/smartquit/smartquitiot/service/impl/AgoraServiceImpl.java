package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.service.AgoraService;
import com.smartquit.smartquitiot.util.agora.RtcTokenBuilder2;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;


@Service
@RequiredArgsConstructor
public class AgoraServiceImpl implements AgoraService {

    @Value("${agora.appId}")
    private String appId;

    @Value("${agora.appCertificate}")
    private String appCertificate;

    @Override
    public String generateRtcToken(String channelName, int uid, int ttlSeconds) {
        try {
            RtcTokenBuilder2 builder = new RtcTokenBuilder2();
            // expire epoch (seconds)
            long expireEpoch = Instant.now().getEpochSecond() + Math.max(30, ttlSeconds);

            // RtcTokenBuilder2.buildTokenWithUid(...) signature in your project previously accepted tokenExpire & privilegeExpire as int epoch
            return builder.buildTokenWithUid(
                    appId,
                    appCertificate,
                    channelName,
                    uid,
                    RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                    (int) expireEpoch,
                    (int) expireEpoch
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo token Agora", e);
        }
    }
}

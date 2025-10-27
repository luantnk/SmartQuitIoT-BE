package com.smartquit.smartquitiot.service;

public interface AgoraService {
    String generateRtcToken(String channel, int uid, int ttlSeconds);
}
